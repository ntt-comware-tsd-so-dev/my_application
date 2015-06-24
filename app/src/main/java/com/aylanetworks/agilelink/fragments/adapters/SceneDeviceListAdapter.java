package com.aylanetworks.agilelink.fragments.adapters;

import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;

import com.aylanetworks.aaml.zigbee.AylaSceneZigbee;
import com.aylanetworks.aaml.zigbee.AylaSceneZigbeeNodeEntity;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.Gateway;
import com.aylanetworks.agilelink.framework.GenericDeviceViewHolder;

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

    Gateway _gateway;
    AylaSceneZigbee _scene;

    private static List<Device> getDeviceList(Gateway gateway, AylaSceneZigbee scene) {
        if ((gateway != null) && (scene != null)) {
            return gateway.getDevicesForScene(scene);
        }
        return new ArrayList<Device>();
    }

    public SceneDeviceListAdapter(Gateway gateway, AylaSceneZigbee scene, View.OnClickListener listener) {
        super(getDeviceList(gateway, scene), listener);
        _gateway = gateway;
        _scene = scene;
    }

    public AylaSceneZigbeeNodeEntity getDeviceEntity(Device device) {
        String dsn = device.getDeviceDsn();
        for (AylaSceneZigbeeNodeEntity nodeEntity : _scene.nodes) {
            if (TextUtils.equals(dsn, nodeEntity.dsn)) {
                return nodeEntity;
            }
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final GenericDeviceViewHolder h = (GenericDeviceViewHolder) holder;
        Device d = _deviceList.get(position);
        h._sceneDeviceEntity = getDeviceEntity(d);

        // Set the onClickListener for this view and set the index as the tag so we can
        // retrieve it later
        holder.itemView.setOnClickListener(_onClickListener);
        holder.itemView.setTag(position);
        d.bindViewHolder(holder);
    }
}
