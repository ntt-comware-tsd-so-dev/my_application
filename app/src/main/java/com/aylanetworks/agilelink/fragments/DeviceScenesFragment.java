package com.aylanetworks.agilelink.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Message;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.aaml.zigbee.AylaSceneZigbee;
import com.aylanetworks.aaml.zigbee.AylaSceneZigbeeNodeEntity;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.device.ZigbeeGateway;
import com.aylanetworks.agilelink.device.ZigbeeSwitchedDevice;
import com.aylanetworks.agilelink.fragments.adapters.SceneDeviceListAdapter;
import com.aylanetworks.agilelink.fragments.adapters.SceneDeviceSelectionAdapter;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.DeviceManager;
import com.aylanetworks.agilelink.framework.Gateway;
import com.aylanetworks.agilelink.framework.Logger;
import com.aylanetworks.agilelink.framework.SessionManager;
import com.aylanetworks.agilelink.framework.ZigbeeSceneManager;

import java.util.ArrayList;
import java.util.List;

/*
 * DeviceGroupsFragment.java
 * AgileLink Application Framework
 *
 * Created by David N. Junod on 6/19/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class DeviceScenesFragment extends AllDevicesFragment implements DeviceManager.GetDeviceComparable {
    private static final String LOG_TAG = "DeviceScenesFragment";

    protected HorizontalScrollView _buttonScrollView;
    protected String _selectedSceneName;

    public static DeviceScenesFragment newInstance() {
        return new DeviceScenesFragment();
    }

    public DeviceScenesFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        LinearLayout l = (LinearLayout) root.findViewById(R.id.button_tray);
        l.setVisibility(View.VISIBLE);
        _buttonScrollView = (HorizontalScrollView) root.findViewById(R.id.button_scroll_view);
        return root;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_scenes, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_modify_scene:
                onAddDeviceToScene();
                break;

            case R.id.action_add_scene:
                onAddScene();
                break;

            case R.id.action_activate_scene:
                onActivateScene();
                break;

            case R.id.action_delete_scene:
                onDeleteScene();
                break;

            case R.id.action_add_device:
                onAddDeviceToScene();
                break;

            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @Override
    public boolean isDeviceComparableType(Device another) {
        return (another instanceof ZigbeeSwitchedDevice);
    }

    @Override
    public void updateDeviceList() {
        _adapter = new SceneDeviceListAdapter(_selectedSceneName, this);
        _recyclerView.setAdapter(_adapter);
        if (_selectedSceneName != null) {
            if (_adapter.getItemCount() == 0) {
                _emptyView.setText(R.string.no_devices_in_scene);
                _recyclerView.setVisibility(View.GONE);
                _emptyView.setVisibility(View.VISIBLE);
            } else {
                _recyclerView.setVisibility(View.VISIBLE);
                _emptyView.setVisibility(View.GONE);
            }
        } else {
            _recyclerView.setVisibility(View.GONE);
            _emptyView.setText(R.string.scene_empty_text);
            _emptyView.setVisibility(View.VISIBLE);
        }
    }

    protected void createSceneButtonHeader() {
        List<String> sceneNames = ZigbeeSceneManager.getSceneNames();
        if (TextUtils.isEmpty(_selectedSceneName) && !sceneNames.isEmpty()) {
            _selectedSceneName = sceneNames.get(0);
        }
        Logger.logDebug(LOG_TAG, "zs: " + sceneNames.size() + " scenes");

        int headerMargin = (int)getResources().getDimension(R.dimen.group_header_margin);
        int buttonMargin = (int)getResources().getDimension(R.dimen.group_button_margin);
        int buttonPadding = (int)getResources().getDimension(R.dimen.group_button_padding);
        Log.d("DIMENS", "hm: " + headerMargin + " bm: " + buttonMargin + " bp: " + buttonPadding);

        // Make a linear layout to hold all of the buttons
        LinearLayout layout = new LinearLayout(getActivity());
        layout.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        layoutParams.setMargins(headerMargin, headerMargin, headerMargin, headerMargin);
        layoutParams.gravity = Gravity.CENTER_VERTICAL;
        layout.setLayoutParams(layoutParams);

        for (String name : sceneNames) {
            Logger.logDebug(LOG_TAG, "zs: scene " + name);

            Button b = new Button(getActivity());

            layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.setMargins(buttonMargin, 0, buttonMargin, 0);
            layoutParams.gravity = Gravity.CENTER_VERTICAL;
            b.setLayoutParams(layoutParams);

            b.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
            b.setPadding(buttonPadding, buttonPadding, buttonPadding, buttonPadding);

            b.setText(name);
            b.setTag(name);
            b.setLayoutParams(layoutParams);
            b.setBackground(getResources().getDrawable(R.drawable.toggle_button_bg));

            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LinearLayout l = (LinearLayout) v.getParent();
                    for (int i = 0; i < l.getChildCount(); i++) {
                        l.getChildAt(i).setSelected(false);
                    }
                    v.setSelected(true);
                    onSceneSelected((String) v.getTag());
                }
            });

            b.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    v.performClick();
                    return true;
                }
            });

            b.setSelected(TextUtils.equals(_selectedSceneName, name));
            layout.addView(b);
        }

        _buttonScrollView.removeAllViews();
        _buttonScrollView.addView(layout);
    }

    protected SceneDeviceSelectionAdapter newSceneDeviceSelectionAdapter(List<Device> list) {
        Device[] objects = new Device[list.size()];
        list.toArray(objects);
        return new SceneDeviceSelectionAdapter(getActivity(), objects);
    }

    Object fetchLock = new Object();
    int fetchCount;
    int fetchDone;

    void fetchScenesComplete() {
        if (isAdded()) {
            createSceneButtonHeader();
            updateDeviceList();
        }
    }

    void fetchScenes() {
        // TODO: should do this in a way that checks if we have actually gotten the scenes
        if (SessionManager.deviceManager() == null) {
            Log.d(LOG_TAG, "Not yet ready to create scene buttons...");
            return;
        }

        synchronized (fetchLock) {
            // get the scenes for all the gateways
            final List<Gateway> gateways = SessionManager.deviceManager().getGatewayDevices();
            if ((gateways != null) && (gateways.size() > 0)) {
               // fetchCount = gateways.size();
                fetchCount = getZigbeeGatewayCount();
                fetchDone = 0;
                for (Gateway g : gateways) {
                    if (g.isZigbeeGateway()) {
                        ZigbeeGateway gateway = (ZigbeeGateway)g;
                        gateway.fetchScenes(this, new Gateway.AylaGatewayCompletionHandler() {
                            @Override
                            public void gatewayCompletion(Gateway gateway, Message msg, Object tag) {
                                fetchDone++;
                                if (fetchDone == fetchCount) {
                                    fetchScenesComplete();
                                }
                            }
                        });
                    }
                }
            } else {
                fetchScenesComplete();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (SessionManager.deviceManager() != null) {
            fetchScenes();
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.add_button) {
            if (TextUtils.isEmpty(_selectedSceneName)) {
                // There is no scene yet. We need to offer to add one.
                onAddScene();
                return;
            }

            // Put up a menu to see if they want to add a device or a scene.
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setIcon(R.drawable.ic_launcher)
                .setTitle(R.string.add_device_or_scene_title)
                .setItems(R.array.device_or_scene_items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            // Add a device to this group
                            onAddDeviceToScene();
                        } else {
                            onAddScene();
                        }
                    }
                })
                .create()
                .show();
        } else {
            int itemIndex = (int)v.getTag();
            final Device d = _adapter.getItem(itemIndex);
            if (d.isIcon()) {
                // recall scene
                onActivateScene();
            } else {
                super.onClick(v);
            }
        }
    }

    @Override
    public void deviceListChanged() {
        super.deviceListChanged();
        createSceneButtonHeader();
        updateDeviceList();
    }

    static class SceneAction implements Gateway.AylaGatewayCompletionHandler {

        enum SceneActionStartMode {
            None,
            Sync,
            Async,
        }

        List<Gateway> gateways;
        int index;

        int countTotal;
        int countDone;
        int countSuccess;
        List<String> failedNodes;

        Object _tag;
        Gateway.AylaGatewayActionHandler _handler;

        public SceneAction(Object tag, Gateway.AylaGatewayActionHandler handler, SceneActionStartMode mode) {
            gateways = SessionManager.deviceManager().getGatewayDevices();
            failedNodes = new ArrayList<>();
            //countTotal = gateways.size();
            countTotal = countDone = countSuccess = 0;
            countTotal = getZigbeeGatewayCount();
            _tag = tag;
            _handler = handler;
            if (mode == SceneActionStartMode.Async) {
                startAll();
            } else if (mode == SceneActionStartMode.Sync) {
                startSync();
            }
        }

        public void startSync() {
            index = 0;
            process();
        }

        public void startAll() {
            index = countTotal;
            for (int i = 0; i < countTotal; i++) {
                Gateway gateway = gateways.get(i);
                _handler.performAction(this, gateway, _tag);
            }
        }

        public void actionComplete(Message msg) {
            if (msg.what == AylaNetworks.AML_ERROR_OK) {
                countSuccess++;
            }
            countDone++;
            if (countDone == countTotal) {
                complete(msg);
            } else {
                process();
            }
        }

        void process() {
            if (index < countTotal) {
                Gateway gateway = gateways.get(index++);
                _handler.performAction(this, gateway, _tag);
            }
        }

        void complete(Message msg) {
            _handler.complete(this, msg, _tag);
        }

        @Override
        public void gatewayCompletion(Gateway gateway, Message msg, Object tag) {
            actionComplete(msg);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // update scene

    int updateSceneGatewayCount;
    int updateSceneGatewayDone;
    int updateSceneGatewaySuccess;

    protected void updateSceneComplete(final String name, final List<Device> sceneDevices, List<String> failedNodes) {
        MainActivity.getInstance().dismissWaitDialog();
        if (updateSceneGatewaySuccess == updateSceneGatewayCount) {

            if(failedNodes.isEmpty()){
                Toast.makeText(getActivity(), R.string.scene_update_complete, Toast.LENGTH_LONG).show();
            } else{
                Toast.makeText(getActivity(), getResources().getString(R.string.scene_update_partial_success) + " " + failedNodes.toString(), Toast.LENGTH_LONG).show();
            }

            _selectedSceneName = name;
            deviceListChanged();
        } else {
            Toast.makeText(getActivity(), R.string.scene_update_failed, Toast.LENGTH_LONG).show();
        }
    }

    protected void updateSceneDevices(final String name, final List<Device> sceneDevices) {
        MainActivity.getInstance().showWaitDialog(R.string.scene_update_title, R.string.scene_update_body);

        List<Gateway> gateways = SessionManager.deviceManager().getGatewayDevices();
        failedNodes = new ArrayList<>();
        updateSceneGatewayCount =getZigbeeGatewayCount();
        updateSceneGatewayDone = updateSceneGatewaySuccess = 0;

        for (Gateway g : gateways) {
            if (g.isZigbeeGateway()) {
                ZigbeeGateway gateway = (ZigbeeGateway)g;
                List<Device> devices = gateway.filterDeviceList(sceneDevices);
                final AylaSceneZigbee scene = gateway.getSceneByName(name);
                Logger.logInfo(LOG_TAG, "zs: updateScene [%s] [%s:%s]", name, gateway.getDeviceDsn(), DeviceManager.deviceListToString(sceneDevices));
                if (scene == null) {
                    gateway.createScene(name, devices, this, new Gateway.AylaGatewayCompletionHandler() {
                        @Override
                        public void gatewayCompletion(Gateway gateway, Message msg, Object tag) {
                            if (msg.what == AylaNetworks.AML_ERROR_OK) {
                                updateSceneGatewaySuccess++;
                            }
                            updateSceneGatewayDone++;
                            if (updateSceneGatewayDone == updateSceneGatewayCount) {
                                updateSceneComplete(name, sceneDevices, failedNodes);
                            }
                        }
                    });
                } else {
                    gateway.updateSceneDevices(scene, devices, this, new Gateway.AylaGatewayCompletionHandler() {
                        @Override
                        public void gatewayCompletion(Gateway gateway, Message msg, Object tag) {
                            if(msg.what == AylaNetworks.AML_ERROR_OK) {
                                if (msg.what == AylaNetworks.AML_ERROR_OK) {
                                    if(msg.arg1 == 206){
                                        AylaSceneZigbee updatedScene = AylaSystemUtils.gson.fromJson((String) msg.obj, AylaSceneZigbee.class);
                                        failedNodes = getFailedNodes(updatedScene);
                                    }
                                }
                                updateSceneGatewaySuccess++;
                            }
                            updateSceneGatewayDone++;
                            if (updateSceneGatewayDone == updateSceneGatewayCount) {
                                updateSceneComplete(name, sceneDevices, failedNodes);
                            }
                        }
                    });
                }
            }
        }
    }

    protected void onAddDeviceToScene() {

        // get a list of available devices
        final List<Device> allDevices = SessionManager.deviceManager().getDevicesOfComparableType(this);
        if (allDevices.isEmpty()) {
            Toast.makeText(getActivity(), R.string.no_devices, Toast.LENGTH_SHORT).show();
            return;
        }

        // get a list of current devices in the scenee
        List<Device> sceneDevices = ZigbeeSceneManager.getDevicesForSceneName(_selectedSceneName);

        final String deviceNames[] = new String[allDevices.size()];
        final boolean isSceneMember[] = new boolean[allDevices.size()];

        for (int i = 0; i < allDevices.size(); i++) {
            Device d = allDevices.get(i);
            deviceNames[i] = d.toString();
            isSceneMember[i] = DeviceManager.isDsnInDeviceList(d.getDeviceDsn(), sceneDevices);
        }

        // TODO: needs to use newSceneDeviceSelectionAdapter

        new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.ic_launcher)
                .setTitle(R.string.choose_scene_devices)
                .setMultiChoiceItems(deviceNames, isSceneMember, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        isSceneMember[which] = isChecked;
                    }
                })
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        List<Device> newSceneList = new ArrayList<>();
                        for (int i = 0; i < allDevices.size(); i++) {
                            Device d = allDevices.get(i);
                            if (isSceneMember[i]) {
                                newSceneList.add(d);
                            }
                        }
                        updateSceneDevices(_selectedSceneName, newSceneList);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create().show();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // add scene

    int addSceneGatewayCount;
    int addSceneGatewayDone;
    int addSceneGatewaySuccess;
    List<String> failedNodes;

    protected void addSceneComplete(final String name, final List<Device> sceneDevices, List<String> failedNodes) {
        MainActivity.getInstance().dismissWaitDialog();
        if (addSceneGatewaySuccess == addSceneGatewayCount) {
            if(failedNodes.isEmpty()){
                Toast.makeText(getActivity(), R.string.scene_create_complete, Toast.LENGTH_LONG).show();
            } else{
                Toast.makeText(getActivity(), getResources().getString(R.string.scene_create_partial_success) + " "+ failedNodes.toString() , Toast.LENGTH_LONG).show();
            }
            _selectedSceneName = name;
            deviceListChanged();
        } else {
            Toast.makeText(getActivity(), R.string.scene_create_failed, Toast.LENGTH_LONG).show();
        }
    }

    // Create the scene across all gateways, regardless of whether any devices are selected
    protected void addScene(final String name, final List<Device> sceneDevices) {
        MainActivity.getInstance().showWaitDialog(R.string.scene_create_title, R.string.scene_create_body);

        List<Gateway> gateways = SessionManager.deviceManager().getGatewayDevices();
        failedNodes = new ArrayList<>();
        //addSceneGatewayCount = gateways.size();
        addSceneGatewayCount = addSceneGatewayDone = addSceneGatewaySuccess = 0;
        addSceneGatewayCount = getZigbeeGatewayCount();

        Logger.logInfo(LOG_TAG, "zs: addScene [%s] [%s]", name,  DeviceManager.deviceListToString(sceneDevices));
        for (Gateway g : gateways) {
            if (g.isZigbeeGateway()) {
                ZigbeeGateway gateway = (ZigbeeGateway)g;
                final List<Device> devices = new ArrayList<Device>( gateway.filterDeviceList(sceneDevices));
                Logger.logInfo(LOG_TAG, "zs: addScene [%s] [%s:%s]", name, gateway.getDeviceDsn(), DeviceManager.deviceListToString(devices));
                gateway.createScene(name, devices, this, new Gateway.AylaGatewayCompletionHandler() {
                    @Override
                    public void gatewayCompletion(Gateway gateway, Message msg, Object tag) {
                        if (msg.what == AylaNetworks.AML_ERROR_OK) {
                            if(msg.arg1 == 206){
                                AylaSceneZigbee scene = AylaSystemUtils.gson.fromJson((String) msg.obj, AylaSceneZigbee.class);
                                failedNodes = getFailedNodes(scene);
                            }
                            addSceneGatewaySuccess++;
                        }
                        addSceneGatewayDone++;
                        if (addSceneGatewayDone == addSceneGatewayCount) {
                            addSceneComplete(name, sceneDevices, failedNodes);
                        }
                    }
                });
            }
        }
    }

    // Should show only the allowed characters in the soft keyboard too
    // http://www.infiniterecursion.us/2011/02/android-activity-custom-keyboard.html

    // These are the only characters allowed in a Scene name.
    protected InputFilter acceptedFilter = new InputFilter() {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            if (source.equals("")) {
                return source;
            }
            // Spaces are not allowed in Scene names, but the underscore is.  We replace
            // spaces with underscores.
            if (source.toString().matches("[a-zA-Z0-9[- \\/\\(\\)\\{\\}\\[\\]\\#\\@\\$]]*")) {
                return source;
            }
            return "";
        }
    };

    protected class DoneOnEditorActionListener implements TextView.OnEditorActionListener {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                InputMethodManager imm = (InputMethodManager)v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                return true;
            }
            return false;
        }
    }

    protected void onAddSceneAdvanced() {
        // we are going to create scenes across gateways
        final List<Device> devices = SessionManager.deviceManager().getDevicesOfComparableType(this);
        if (devices.isEmpty()) {
            Toast.makeText(getActivity(), R.string.no_devices, Toast.LENGTH_SHORT).show();
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        final View alertView = inflater.inflate(R.layout.dialog_add_scene_list, null);
        final EditText et = (EditText)alertView.findViewById(R.id.scene_name);
        et.setFilters(new InputFilter[] { acceptedFilter});
        et.setOnEditorActionListener(new DoneOnEditorActionListener());

        final SceneDeviceSelectionAdapter adapter = newSceneDeviceSelectionAdapter(devices);
        ListView listView = (ListView)alertView.findViewById(R.id.list);
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        final AlertDialog dlg = new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.ic_launcher)
                .setTitle(R.string.add_scene_title)
                .setView(alertView)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dlg.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface dialog) {

                Button b = dlg.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        String name = et.getText().toString();
                        if (TextUtils.isEmpty(name)) {
                            et.requestFocus();
                        } else {
                            addScene(name, adapter.getSelectedDevices());
                            dlg.dismiss();
                        }
                    }
                });
            }
        });
        dlg.show();
    }

    protected void onAddScene() {
        onAddSceneAdvanced();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // recall scene

    SceneAction actionActivate;

    protected void onActivateScene() {
        if (!TextUtils.isEmpty(_selectedSceneName)) {
            if (actionActivate == null) {
                MainActivity.getInstance().showWaitDialog(R.string.scene_recall_title, R.string.scene_recall_body);
                actionActivate = new SceneAction(_selectedSceneName, new Gateway.AylaGatewayActionHandler() {
                    @Override
                    public void performAction(Object action, Gateway g, Object tag) {
                        if (g.isZigbeeGateway()) {
                            ZigbeeGateway gateway = (ZigbeeGateway) g;
                            if (gateway != null) {
                                AylaSceneZigbee scene = gateway.getSceneByName((String) tag);
                                if (scene != null) {
                                    gateway.recallScene(scene, this, (SceneAction) action);
                                }
                            }
                        } else {
                            Logger.logError(LOG_TAG, "zg: gateway [%s] is not a ZigbeeGateway!", g.getDeviceDsn());
                        }
                    }

                    @Override
                    public void complete(Object action, Message msg, Object tag) {
                        MainActivity.getInstance().dismissWaitDialog();
                        if (AylaNetworks.succeeded(msg)) {
                            Toast.makeText(getActivity(), R.string.scene_recall_complete, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getActivity(), R.string.scene_recall_failed, Toast.LENGTH_LONG).show();
                        }
                        actionActivate = null;
                    }
                }, SceneAction.SceneActionStartMode.Async);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // delete scene

    SceneAction actionDelete;

    protected void deleteScene(final String sceneName) {
        if (!TextUtils.isEmpty(sceneName)) {
            if (actionDelete == null) {
                MainActivity.getInstance().showWaitDialog(R.string.scene_delete_title, R.string.scene_delete_body);
                actionDelete = new SceneAction(sceneName, new Gateway.AylaGatewayActionHandler() {
                    @Override
                    public void performAction(Object action, Gateway g, Object tag) {
                        if (g.isZigbeeGateway()) {
                            ZigbeeGateway gateway = (ZigbeeGateway) g;
                            if (gateway != null) {
                                AylaSceneZigbee scene = gateway.getSceneByName((String) tag);
                                if (scene != null) {
                                    gateway.deleteScene(scene, this, (SceneAction) action);
                                }
                            }
                        }
                    }

                    @Override
                    public void complete(Object action, Message msg, Object tag) {
                        MainActivity.getInstance().dismissWaitDialog();
                        if (AylaNetworks.succeeded(msg)) {
                            Toast.makeText(getActivity(), R.string.scene_delete_complete, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getActivity(), R.string.scene_delete_failed, Toast.LENGTH_LONG).show();
                        }
                        _selectedSceneName = null;
                        deviceListChanged();
                        actionDelete = null;
                    }
                }, SceneAction.SceneActionStartMode.Async);
            }
        }
    }

    protected void onDeleteScene() {
        Log.d(LOG_TAG, "onDeleteScene");
        String msg = getResources().getString(R.string.confirm_delete_scene_body, _selectedSceneName);
        new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.ic_launcher)
                .setTitle(R.string.confirm_delete_scene)
                .setMessage(msg)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteScene(_selectedSceneName);
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .create().show();
    }

    protected void onSceneSelected(String sceneName) {
        Log.d(LOG_TAG, "Selected scene: " + sceneName);
        _selectedSceneName = sceneName;
        updateDeviceList();
    }


    private static int getZigbeeGatewayCount(){
        int count = 0;
        List<Gateway> gateways = SessionManager.deviceManager().getGatewayDevices();
        for(Gateway g: gateways){
            if(g.isZigbeeGateway()){
                count++;
            }
        }
        return count;
    }

    private static List<String> getFailedNodes(AylaSceneZigbee scene){
        List<String> failedNodes = new ArrayList<>();
        AylaSceneZigbeeNodeEntity[] nodes = scene.nodes;

        for(AylaSceneZigbeeNodeEntity node: nodes){
            if(node.errorCode != null){
                if(node.errorCode.equals("ZE_ZCB_COMMS_FAILED")){
                    failedNodes.add(node.dsn);
                }
            }
        }
        return failedNodes;
    }
}
