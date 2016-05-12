package com.aylanetworks.agilelink.fragments.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.aylanetworks.aylasdk.zigbee.AylaSceneZigbeeNodeEntity;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.device.DeviceUIProvider;
import com.aylanetworks.agilelink.device.GenericDevice;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.GenericDeviceViewHolder;
import com.aylanetworks.agilelink.framework.ZigbeeSceneManager;

import java.util.ArrayList;
import java.util.List;

/*
 * DeviceListAdapter.java
 * AgileLink Application Framework
 *
 * Created by David N. Junod on 06/23/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class SceneDeviceListAdapter extends DeviceListAdapter {
    private final static String LOG_TAG = "SceneDeviceListAdapter";

    String _sceneName;

    public static class RecallSceneIcon extends GenericDevice {

        String _sceneName;
        String _label;

        public RecallSceneIcon(String sceneName) {
            super(null);
            _sceneName = sceneName;
            _label = MainActivity.getInstance().getResources().getString(R.string.activate_scene_icon, _sceneName);
        }

        @Override
        public boolean isIcon() {
            return true;
        }

        @Override
        public String getProductName() {
            return _label;
        }

        @Override
        public String getDeviceState() {
            return "";
        }
    }

    static List<DeviceUIProvider> getDevicesForSceneName(String sceneName) {
        List<GenericDevice> list = GenericDevice.fromDeviceList(
                ZigbeeSceneManager.getDevicesForSceneName(sceneName));
        List<DeviceUIProvider> providers = new ArrayList<>(list.size());
        for ( GenericDevice d : list) {
            providers.add(d);
        }
        providers.add(0, new RecallSceneIcon(sceneName));
        return providers;
    }

    public SceneDeviceListAdapter(String sceneName, View.OnClickListener listener) {
        super(getDevicesForSceneName(sceneName), listener);
        _sceneName = sceneName;
    }

    public AylaSceneZigbeeNodeEntity getDeviceEntity(Device device) {
        return ZigbeeSceneManager.getDeviceEntity(_sceneName, device);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final GenericDeviceViewHolder h = (GenericDeviceViewHolder) holder;
        DeviceUIProvider uiProvider = _deviceList.get(position);
        Device d = (Device)uiProvider;
        if (d.isIcon()) {
            //
        } else {
            h._sceneDeviceEntity = getDeviceEntity(d);
        }

        // Set the onClickListener for this view and set the index as the tag so we can
        // retrieve it later
        holder.itemView.setOnClickListener(_onClickListener);
        holder.itemView.setTag(position);
        uiProvider.bindViewHolder(holder);
    }
}
