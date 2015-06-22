package com.aylanetworks.agilelink.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Message;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.zigbee.AylaSceneZigbee;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.device.ZigbeeSwitchedDevice;
import com.aylanetworks.agilelink.fragments.adapters.DeviceListAdapter;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.DeviceManager;
import com.aylanetworks.agilelink.framework.Gateway;
import com.aylanetworks.agilelink.framework.Logger;
import com.aylanetworks.agilelink.framework.SessionManager;

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

    private HorizontalScrollView _buttonScrollView;

    private AylaSceneZigbee _selectedScene;
    private Gateway _selectedSceneGateway;

    public static DeviceScenesFragment newInstance() {
        return new DeviceScenesFragment();
    }

    public DeviceScenesFragment() {
    }

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
        createSceneButtonHeader();
        updateDeviceList();
        return root;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_scenes, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // Change the name of the "Add Device" menu item
        MenuItem addItem = menu.findItem(R.id.action_add_device);
        if ( addItem != null ) {
            addItem.setTitle(R.string.action_manage_devices_in_scene);
        }
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
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

    List<AylaSceneZigbee> getScenes() {
        List<AylaSceneZigbee> scenes = new ArrayList<>();
        if (SessionManager.deviceManager() != null) {
            // get the scenes for all the gateways
            List<Gateway> gateways = SessionManager.deviceManager().getGatewayDevices();
            for (Gateway gateway : gateways) {
                List<AylaSceneZigbee> gs = gateway.getScenes();
                if ((gs != null) && (gs.size() > 0)) {
                    scenes.addAll(gs);
                }
            }
        }
        return scenes;
    }

    protected void updateDeviceList() {
        if (_selectedScene != null) {
            List<Device> selectedGroupDeviceList = _selectedSceneGateway.getDevicesForScene(_selectedScene);
            _adapter = new DeviceListAdapter(selectedGroupDeviceList, this);
            _recyclerView.setAdapter(_adapter);
            if ( selectedGroupDeviceList.isEmpty() ) {
                _emptyView.setText(R.string.no_devices_in_scene);
                _recyclerView.setVisibility(View.GONE);
                _emptyView.setVisibility(View.VISIBLE);
            } else {
                _recyclerView.setVisibility(View.VISIBLE);
                _emptyView.setVisibility(View.GONE);
            }
        } else {
            _adapter = new DeviceListAdapter(null, this);
            _recyclerView.setAdapter(_adapter);
            _recyclerView.setVisibility(View.GONE);
            _emptyView.setText(R.string.scene_empty_text);
            _emptyView.setVisibility(View.VISIBLE);
        }
    }

    protected void createSceneButtonHeader() {
        if (SessionManager.deviceManager() == null) {
            Log.d(LOG_TAG, "Not yet ready to create scene buttons...");
            return;
        }

        List<AylaSceneZigbee> scenes = getScenes();
        if (_selectedScene == null && scenes.size() > 0) {
            _selectedScene = scenes.get(0);
            _selectedSceneGateway =  Gateway.getGatewayForScene(_selectedScene);
        }
        Logger.logDebug(LOG_TAG, "zs: " + scenes.size() + " scenes");

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

        for (AylaSceneZigbee scene : scenes) {
            Logger.logDebug(LOG_TAG, "zs: scene " + scene);

            Button b = new Button(getActivity());

            layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.setMargins(buttonMargin, 0, buttonMargin, 0);
            layoutParams.gravity = Gravity.CENTER_VERTICAL;
            b.setLayoutParams(layoutParams);

            b.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
            b.setPadding(buttonPadding, buttonPadding, buttonPadding, buttonPadding);

            b.setText(scene.sceneName);
            b.setTag(scene);
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
                    onSceneSelected((AylaSceneZigbee) v.getTag());
                }
            });

            b.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    v.performClick();
                    return true;
                }
            });

            b.setSelected(scene.equals(_selectedScene));
            layout.addView(b);
        }

        _buttonScrollView.removeAllViews();
        _buttonScrollView.addView(layout);
    }

    @Override
    public void onResume() {
        super.onResume();
        createSceneButtonHeader();
        updateDeviceList();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.add_button) {
            if ( _selectedScene == null ) {
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
            super.onClick(v);
        }
    }

    @Override
    public void deviceListChanged() {
        super.deviceListChanged();
        createSceneButtonHeader();
        updateDeviceList();
    }

    void updateSceneDevices(List<Device> newSceneList) {
        MainActivity.getInstance().showWaitDialog(R.string.scene_update_title, R.string.scene_update_body);
        _selectedSceneGateway.updateSceneDevices(_selectedScene, newSceneList, this, new Gateway.AylaGatewayCompletionHandler() {
            @Override
            public void gatewayCompletion(Gateway gateway, Message msg, Object tag) {
                MainActivity.getInstance().dismissWaitDialog();
                updateDeviceList();
                Toast.makeText(getActivity(), R.string.scene_update_complete, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public boolean isDeviceComparableType(Device another) {
        return (another instanceof ZigbeeSwitchedDevice);
    }

    protected void onAddDeviceToScene() {
        // only devices for this gateway
        List<Device> sceneDevices = _selectedSceneGateway.getDevicesForScene(_selectedScene);
        final List<Device> allDevices = _selectedSceneGateway.getDevicesOfComparableType(this);
        final String deviceNames[] = new String[allDevices.size()];
        final boolean isSceneMember[] = new boolean[allDevices.size()];

        for (int i = 0; i < allDevices.size(); i++) {
            Device d = allDevices.get(i);
            deviceNames[i] = d.toString();
            isSceneMember[i] = DeviceManager.isDsnInDeviceList(d.getDeviceDsn(), sceneDevices);
        }

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
                        updateSceneDevices(newSceneList);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create().show();
    }

    void addSceneAdvanced() {
        List<Gateway> gateways = SessionManager.deviceManager().getGatewayDevices();
        // Instruct the user to setup the scene the way that they want it... with the lights
        // and plugs in the state that they want.
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        final View alertView = inflater.inflate(R.layout.dialog_add_scene, null);
        ListView list = (ListView)alertView.findViewById(R.id.list);

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
                    }
                });
            }
        });
        dlg.show();
    }

    void addScene(final Gateway gateway, final String name) {
        if (TextUtils.isEmpty(name)) {
            Logger.logError(LOG_TAG, "zs: no name entered!");
            return;
        }
        MainActivity.getInstance().showWaitDialog(R.string.scene_create_title, R.string.scene_create_body);
        gateway.createScene(name, null, this, new Gateway.AylaGatewayCompletionHandler() {
            @Override
            public void gatewayCompletion(Gateway gateway, Message msg, Object tag) {
                MainActivity.getInstance().dismissWaitDialog();
                if (AylaNetworks.succeeded(msg)) {
                    Toast.makeText(getActivity(), R.string.scene_create_complete, Toast.LENGTH_LONG).show();
                    _selectedScene = gateway.getSceneByName(name);
                    _selectedSceneGateway = gateway;
                    updateDeviceList();
                    onAddDeviceToScene();
                } else {
                    Toast.makeText(getActivity(), R.string.scene_create_failed, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    // Should show only the allowed characters in the soft keyboard too
    // http://www.infiniterecursion.us/2011/02/android-activity-custom-keyboard.html

    // These are the only characters allowed in a Scene name.
    InputFilter acceptedFilter = new InputFilter() {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            if (source.equals("")) {
                return source;
            }
            if (source.toString().matches("[a-zA-Z0-9[-_\\/\\(\\)\\{\\}\\[\\]\\#\\@\\$]]*")) {
                return source;
            }
            return "";
        }
    };

    protected void onAddScene() {
        // need to select which gateway if there are multiple
        List<Gateway> gateways = SessionManager.deviceManager().getGatewayDevices();
        final Gateway gateway = gateways.isEmpty() ? null: gateways.get(0);
        if (gateway == null) {
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        final View alertView = inflater.inflate(R.layout.dialog_add_scene, null);
        final EditText et = (EditText)alertView.findViewById(R.id.scene_name);
        et.setFilters(new InputFilter[] { acceptedFilter});

        AlertDialog dlg = new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.ic_launcher)
                .setTitle(R.string.add_scene_title)
                .setView(alertView)
                .setPositiveButton(R.string.add_scene, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        addScene(gateway, et.getText().toString());
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dlg.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dlg.show();

    }

    protected void onActivateScene() {
        if ((_selectedSceneGateway != null) && (_selectedScene != null)) {
            MainActivity.getInstance().showWaitDialog(R.string.scene_recall_title, R.string.scene_recall_body);
            _selectedSceneGateway.recallScene(_selectedScene, this, new Gateway.AylaGatewayCompletionHandler() {
                @Override
                public void gatewayCompletion(Gateway gateway, Message msg, Object tag) {
                    MainActivity.getInstance().dismissWaitDialog();
                    if (AylaNetworks.succeeded(msg)) {
                        Toast.makeText(getActivity(), R.string.scene_recall_complete, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getActivity(), R.string.scene_recall_failed, Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

    void deleteScene(AylaSceneZigbee scene) {
        MainActivity.getInstance().showWaitDialog(R.string.scene_delete_title, R.string.scene_delete_body);
        _selectedSceneGateway.deleteScene(scene, this, new Gateway.AylaGatewayCompletionHandler() {
            @Override
            public void gatewayCompletion(Gateway gateway, Message msg, Object tag) {
                MainActivity.getInstance().dismissWaitDialog();
                updateDeviceList();
                if (AylaNetworks.succeeded(msg)) {
                    Toast.makeText(getActivity(), R.string.scene_delete_complete, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getActivity(), R.string.scene_delete_failed, Toast.LENGTH_LONG).show();
                }
                List<AylaSceneZigbee> scenes = getScenes();
                if (scenes.isEmpty()) {
                    _selectedScene = null;
                } else {
                    _selectedScene = scenes.get(0);
                    _selectedSceneGateway = Gateway.getGatewayForScene(_selectedScene);
                }
                deviceListChanged();
            }
        });
    }

    protected void onDeleteScene() {
        Log.d(LOG_TAG, "onDeleteScene");
        String msg = getResources().getString(R.string.confirm_delete_scene_body, _selectedScene.sceneName);
        new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.ic_launcher)
                .setTitle(R.string.confirm_delete_scene)
                .setMessage(msg)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteScene(_selectedScene);
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .create().show();
    }

    protected void onSceneSelected(AylaSceneZigbee scene) {
        Log.d(LOG_TAG, "Selected scene: " + scene);
        _selectedScene = scene;
        _selectedSceneGateway =  Gateway.getGatewayForScene(_selectedScene);
        updateDeviceList();
    }
}
