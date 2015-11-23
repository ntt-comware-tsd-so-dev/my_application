package com.aylanetworks.agilelink.framework;

import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.aaml.zigbee.AylaDeviceZigbeeGateway;
import com.aylanetworks.aaml.zigbee.AylaNetworksZigbee;
import com.aylanetworks.aaml.zigbee.AylaSceneZigbee;
import com.aylanetworks.aaml.zigbee.AylaSceneZigbeeNodeEntity;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
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

/*
 * ZigbeeSceneManager.java
 * AgileLink Application Framework
 *
 * Created by David Junod on 6/18/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class ZigbeeSceneManager {

    private final static String LOG_TAG = "ZigbeeSceneManager";

    private ZigbeeGateway _gateway;
    private Set<ZigbeeSceneManagerListener> _listeners;
    protected Set<AylaSceneZigbee> _scenes;

    /**
     * Static method to get the complete list of scene names across all gateways.
     *
     * @return List of scene names
     */
    public static List<String> getSceneNames() {
        List<String> list = new ArrayList<>();
        List<Gateway> gateways = SessionManager.deviceManager().getGatewayDevices();
        for (Gateway g : gateways) {
            if (g.isZigbeeGateway()) {
                ZigbeeGateway gateway = (ZigbeeGateway)g;
                List<AylaSceneZigbee> scenes = gateway.getScenes();
                if ((scenes != null) && (scenes.size() > 0)) {
                    for (AylaSceneZigbee scene : scenes) {
                        // Not case-sensitive
                        if (!list.contains(scene.sceneName)) {
                            list.add(scene.sceneName.replace("_", " "));
                        }
                    }
                }
            }
        }
        Collections.sort(list, String.CASE_INSENSITIVE_ORDER);
        return list;
    }

    /**
     * Static method to get the device list for a given scene name from across all Gateways.
     * @param sceneName Scene name
     * @return Device list across all Gateways.
     */
    public static List<Device> getDevicesForSceneName(String sceneName) {
        List<Gateway> gateways = SessionManager.deviceManager().getGatewayDevices();
        List<Device> devices = new ArrayList<>();
        if ((gateways != null) && (gateways.size() > 0)) {
            for (Gateway g : gateways) {
                if (g.isZigbeeGateway()) {
                    ZigbeeGateway gateway = (ZigbeeGateway)g;
                    AylaSceneZigbee scene = gateway.getSceneByName(sceneName);
                    if (scene != null) {
                        devices.addAll(gateway.getDevicesForScene(scene));
                    }
                }
            }
        }
        return devices;
    }

    /**
     * Static method to get the AylaSceneZigbeeNodeEntity for a device.
     * @param sceneName Scene name.
     * @param device Device to search for.
     * @return AylaSceneZigbeeNodeEntity for the device.
     */
    public static AylaSceneZigbeeNodeEntity getDeviceEntity(String sceneName, Device device) {
        String dsn = device.getDeviceDsn();
        List<Gateway> gateways = SessionManager.deviceManager().getGatewayDevices();
        if ((gateways != null) && (gateways.size() > 0)) {
            for (Gateway g : gateways) {
                if (g.isZigbeeGateway()) {
                    ZigbeeGateway gateway = (ZigbeeGateway)g;
                    AylaSceneZigbee scene = gateway.getSceneByName(sceneName);
                    if (scene != null) {
                        // need the node entities...
                        if (scene.nodes != null) {
                            for (AylaSceneZigbeeNodeEntity nodeEntity : scene.nodes) {
                                if (TextUtils.equals(dsn, nodeEntity.dsn)) {
                                    return nodeEntity;
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Default constructor
     * @param gateway Gateway
     */
    public ZigbeeSceneManager(ZigbeeGateway gateway) {
        _gateway = gateway;
        _listeners = new HashSet<>();
        _scenes = new HashSet<>();
    };

    // Listeners

    /**
     * Interface to notify of changes to the scene list or scenes within the list
     */
    public interface ZigbeeSceneManagerListener {

        /**
         * Scene list has changed.
         * @param gateway Gateway
         */
        void zigbeeSceneListChanged(Gateway gateway);

        /**
         * Scene members have changed.
         * @param gateway Gateway
         * @param scene AylaSceneZigbee that the members have changed in.
         */
        void zigbeeSceneMembersChanged(Gateway gateway, AylaSceneZigbee scene);

        /**
         * Scene has been created
         * @param gateway Gateway
         * @param name Name of the scene
         * @param msg Ayla Message
         * @param scene AylaSceneZigbee
         */
        void zigbeeCreateSceneCompleted(Gateway gateway, String name, Message msg, AylaSceneZigbee scene);

        /**
         * Scene has been updated.
         * @param gateway Gateway
         * @param name Name of the scene
         * @param msg Ayla Message
         * @param scene AylaSceneZigbee
         */
        void zigbeeUpdateSceneCompleted(Gateway gateway, String name, Message msg, AylaSceneZigbee scene);

        /**
         * Scene has been recalled
         * @param gateway Gateway
         * @param name Name of the scene.
         * @param msg Ayla Message
         * @param scene AylaSceneZigbee
         */
        void zigbeeRecallSceneCompleted(Gateway gateway, String name, Message msg, AylaSceneZigbee scene);

        /**
         * Scene has been deleted.
         * @param gateway Gateway
         * @param name Name of the scene.
         * @param msg Ayla Message.
         */
        void zigbeeDeleteSceneCompleted(Gateway gateway, String name, Message msg);
    }

    /**
     * Add a listener for observing changes to the ZigbeeSceneManager
     * @param listener ZigbeeSceneManagerListener to add.
     */
    public void addListener(ZigbeeSceneManagerListener listener) {
        _listeners.add(listener);
    }

    /**
     * Remove a formerly added observer.
     * @param listener ZigbeeSceneManagerListener to remove.
     */
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
            if (scene.nodes != null) {
                for (AylaSceneZigbeeNodeEntity dn : scene.nodes) {
                    if (sb.length() > 0) {
                        sb.append(",");
                    }
                    sb.append(dn.dsn);
                    if (TextUtils.equals(dn.action, "delete") && TextUtils.equals(dn.status,"success")) {
                        sb.append(" (Deleted)");
                    }
                }
            }
        }
        return sb.toString();
    }

    /**
     * Get a list of the Device devices that are in a AylaSceneZigbee scene.
     * @param scene AylaSceneZigbee
     * @return List of Device devices in the scene.
     */
    static public List<Device> getDevices(AylaSceneZigbee scene) {
        List<Device> list = new ArrayList<>();
        if (scene != null) {
            if (scene.nodes != null) {
                Logger.logDebug(LOG_TAG, "zs: getDevices [%s] nodes", scene.sceneName);
                for (AylaSceneZigbeeNodeEntity dn : scene.nodes) {
                    if (dn == null) {
                        Logger.logWarning(LOG_TAG, "zs: getDevices [%s] dn NULL", scene.sceneName);
                        continue;
                    }
                    if (TextUtils.equals(dn.action, "delete") && TextUtils.equals(dn.status,"success")) {
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
        String dsn = device.getDeviceDsn();
        if (scene.nodes != null) {
            for (AylaSceneZigbeeNodeEntity dn : scene.nodes) {
                if (dn == null) {
                    continue;
                }
                if (TextUtils.equals(dsn, dn.dsn)) {
                    if (TextUtils.equals(dn.action, "delete") && TextUtils.equals(dn.status, "success")) {
                        return false;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Fetch the scene list if it hasn't been fetched before
     */
    public void fetchZigbeeScenesIfNeeded() {
        if ((_scenes == null) || (_scenes.size() == 0)) {
            fetchZigbeeScenes(null, null);
        }
    }

    /**
     * Fetches the list of scenes from the server. Listeners will be notified when the call is
     * complete by being called with zigbeeSceneListChanged() if the scene list has changed.
     * @param tag Optional user tag for completion handler.
     * @param completion Optional completion handler.
     */
    public void fetchZigbeeScenes(Object tag, Gateway.AylaGatewayCompletionHandler completion) {
        Map<String, Object> callParams = new HashMap<>();
        callParams.put(AylaNetworksZigbee.kAylaZigbeeNodeParamDetail, "true");          // default is "false"
        callParams.put(AylaNetworksZigbee.kAylaZigbeeNodeParamStatusFilter, "active");  // default is "active"
        AylaDeviceZigbeeGateway gateway = (AylaDeviceZigbeeGateway)_gateway.getDevice();
        gateway.getScenes(new GetScenesHandler(this, _gateway, tag, completion), callParams, false);
    }

    /**
     * Get the current list of all the AylaSceneZigbee objects.
     * @return List of AylaSceneZigbee objects.
     */
    public List<AylaSceneZigbee> getScenes() {
        return sortSceneSet(_scenes);
    }

    /**
     * Get the AylaSceneZigbee scene with the specified name.
     * @param name String name of the scene to locate.
     * @return AylaSceneZigbee matching the specified name.
     */
    public AylaSceneZigbee getByName(String name) {
        if (!TextUtils.isEmpty(name)) {
            if ((_scenes != null) && (_scenes.size() > 0)) {
                name = name.replace(" ", "_");
                for (AylaSceneZigbee scene : _scenes) {
                    if (TextUtils.equals(scene.sceneName, name)) {
                        return scene;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Compare two scenes to see if they are the same
     * @param scene1 AylaSceneZigbee
     * @param scene2 AylaSceneZigbee
     * @return Returns true if they are the same, false if they are not
     */
    public static boolean isSceneSame(AylaSceneZigbee scene1, AylaSceneZigbee scene2) {
        if (!TextUtils.equals(scene1.sceneHexId, scene2.sceneHexId)) {
            return false;
        }
        if (!TextUtils.equals(scene1.sceneName, scene2.sceneName)) {
            return false;
        }
        if (!TextUtils.equals(scene1.action, scene2.action)) {
            return false;
        }
        if (!TextUtils.equals(scene1.status, scene2.status)) {
            return false;
        }
        if (!TextUtils.equals(scene1.toString(), scene2.toString())) {
            return false;
        }
        return true;
    }

    /**
     * Update the AylaSceneZigbee scene with the specified list of Devices. Just updates the scene's
     * nodeDsns field in preparation for saving with createScene or updateScene.
     * @param scene AylaSceneZigbee to update.
     * @param devices List of Devices for the scene.
     */
    void updateSceneWithDevices(AylaSceneZigbee scene, List<Device> devices) {
        scene.nodes = null;
        if ((devices != null) && (devices.size() > 0)) {
            scene.nodeDsns = new String[devices.size()];
            for (int i = 0; i < devices.size(); i++) {
                scene.nodeDsns[i] = devices.get(i).getDeviceDsn();
            }
        } else {
            scene.nodeDsns = null;
        }
    }

    /**
     * Create a AylaSceneZigbee with the specified name and device list. The current state of the devices
     * in the device list will be captured with the scene.
     * @param name Name for the scene.
     * @param devices List of Devices for the scene.
     * @param tag Optional user data.
     * @param handler Optional completion handler.
     */
    public void createScene(String name, List<Device> devices, Object tag, Gateway.AylaGatewayCompletionHandler handler) {
        name = name.replace(" ", "_");
        AylaSceneZigbee scene = new AylaSceneZigbee();
        scene.sceneName = name;
        scene.gatewayDsn = _gateway.getDeviceDsn();
        updateSceneWithDevices(scene, devices);
        Map<String, Object> callParams = new HashMap<>();
        AylaDeviceZigbeeGateway gateway = (AylaDeviceZigbeeGateway)_gateway.getDevice();
        Logger.logDebug(LOG_TAG, "zs: createScene [%s]", scene);
        gateway.createScene(new CreateHandler(this, name, tag, handler), scene, callParams, false);
    }

    /**
     * Create the specified AylaSceneZigbee. The current state of the devices in the nodeDsns list will be
     * captured with the scene.
     * @param scene AylaSceneZigbee to create.
     * @param tag Optional user data.
     * @param handler Optional completion handler.
     */
    public void createScene(AylaSceneZigbee scene, Object tag, Gateway.AylaGatewayCompletionHandler handler) {
        Map<String, Object> callParams = new HashMap<>();
        AylaDeviceZigbeeGateway gateway = (AylaDeviceZigbeeGateway)_gateway.getDevice();
        gateway.createScene(new CreateHandler(this, scene.sceneName, tag, handler), scene, callParams, false);
    }

    /**
     * Update the specified AylaSceneZigbee with the device list. The current state of the devices in
     * the device list will be captured with the scene.
     * @param scene AylaSceneZigbee to update.
     * @param devices List of Devices
     * @param tag Optional user data.
     * @param handler Optional completion handler.
     */
    public void updateScene(AylaSceneZigbee scene, List<Device> devices, Object tag, Gateway.AylaGatewayCompletionHandler handler) {
        updateSceneWithDevices(scene, devices);
        Map<String, Object> callParams = new HashMap<>();
        AylaDeviceZigbeeGateway gateway = (AylaDeviceZigbeeGateway)_gateway.getDevice();
        gateway.updateScene(new UpdateHandler(this, scene, tag, handler), scene, callParams, false);
    }

    /**
     * Recall the specified AylaSceneZigbee.  The current state of the devices will be updated to reflect
     * the state in the scene's nodes list.
     * @param scene AylaSceneZigbee to recall
     * @param tag Optional user data.
     * @param handler Optional completion handler.
     */
    public void recallScene(AylaSceneZigbee scene, Object tag, Gateway.AylaGatewayCompletionHandler handler) {
        Map<String, Object> callParams = new HashMap<>();
        AylaDeviceZigbeeGateway gateway = (AylaDeviceZigbeeGateway)_gateway.getDevice();
        gateway.recallScene(new RecallHandler(this, scene, tag, handler), scene, callParams, false);
    }

    /**
     * Delete the specified AylaSceneZigbee.
     * @param scene AylaSceneZigbee to delete
     * @param tag Optional user data.
     * @param handler Optional completion handler.
     */
    public void deleteScene(AylaSceneZigbee scene, Object tag, Gateway.AylaGatewayCompletionHandler handler) {
        Map<String, Object> callParams = new HashMap<>();
        AylaDeviceZigbeeGateway gateway = (AylaDeviceZigbeeGateway)_gateway.getDevice();
        gateway.deleteScene(new DeleteHandler(this, scene.sceneName, tag, handler), scene, callParams, false);
    }

    // Internal Handlers

    static class GetScenesHandler extends Handler {
        WeakReference<ZigbeeSceneManager> _manager;
        ZigbeeGateway _gateway;
        Object _tag;
        Gateway.AylaGatewayCompletionHandler _completion;

        GetScenesHandler(ZigbeeSceneManager manager, ZigbeeGateway gateway, Object tag, Gateway.AylaGatewayCompletionHandler completion) {
            _manager = new WeakReference<>(manager);
            _gateway = gateway;
            _tag = tag;
            _completion = completion;
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
            if (_completion != null) {
                _completion.gatewayCompletion(_gateway, msg, _tag);
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
                AylaSceneZigbee scene = AylaSystemUtils.gson.fromJson((String) msg.obj, AylaSceneZigbee.class);
                _manager.get().addScene(scene);
                _manager.get().notifyCreateCompleted(_name, msg, scene);
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
                AylaSceneZigbee scene = AylaSystemUtils.gson.fromJson((String) msg.obj, AylaSceneZigbee.class);
                Logger.logDebug(LOG_TAG, "zs: updateScene [%s]", scene);
                _manager.get().updateScene(scene);
                _manager.get().notifyUpdateCompleted(_name, msg, scene);
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
               //     _manager.get().updateScene(scene);

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

    private List<AylaSceneZigbee> sortSceneSet(Set<AylaSceneZigbee> sceneSet) {
        List<AylaSceneZigbee> scenes = new ArrayList<>(sceneSet);
        Collections.sort(scenes, new Comparator<AylaSceneZigbee>() {
            @Override
            public int compare(AylaSceneZigbee lhs, AylaSceneZigbee rhs) {
                return lhs.sceneName.compareToIgnoreCase(rhs.sceneName);
            }
        });
        return scenes;
    }

    private boolean sceneListChanged(Set<AylaSceneZigbee> newSceneSet) {
        if ((_scenes == null) && (newSceneSet == null)) {
            return false;
        }
        if ((_scenes == null) && (newSceneSet != null)) {
            return true;
        }
        if ((_scenes != null) && (newSceneSet == null)) {
            return true;
        }
        if (_scenes.size() != newSceneSet.size()) {
            return true;
        }
        List<AylaSceneZigbee> scenes1 = sortSceneSet(_scenes);
        List<AylaSceneZigbee> scenes2 = sortSceneSet(newSceneSet);
        for (int i = 0; i < scenes1.size(); i++) {
            AylaSceneZigbee scene1 = scenes1.get(i);
            AylaSceneZigbee scene2 = scenes2.get(i);
            if (!isSceneSame(scene1, scene2)) {
                return true;
            }
        }
        return false;
    }

    private void setScenes(Set<AylaSceneZigbee> set) {
        if (sceneListChanged(set)) {
            _scenes = set;
            notifyListChanged();
        }
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
        String name = sceneName.replace(" ", "_");
        for (AylaSceneZigbee g : _scenes) {
            if (g.sceneName.equals(name)) {
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
