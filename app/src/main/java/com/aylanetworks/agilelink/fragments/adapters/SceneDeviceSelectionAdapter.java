package com.aylanetworks.agilelink.fragments.adapters;
/* 
 * SceneDeviceSelectionAdapter
 * AgileLink Application Framework
 *
 * Created by David N. Junod on 6/23/15.
 * Copyright (c) 2015 Ayla Networks. All rights reserved.
 */

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Switch;
import android.widget.TextView;

import com.aylanetworks.aylasdk.AylaDatapoint;
import com.aylanetworks.aylasdk.AylaProperty;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.Logger;
import com.aylanetworks.agilelink.framework.SessionManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class SceneDeviceSelectionAdapter extends ArrayAdapter<Device> {

    final static String LOG_TAG = "SceneDeviceListAdapter";

    Device[] _devices;
    boolean[] _checks;

    public SceneDeviceSelectionAdapter(Context c, Device[] devices) {
        super(c, R.layout.device_check_property, R.id.device_name, devices);
        _devices = devices;
        _checks = new boolean[devices.length];
    }

    public List<Device> getSelectedDevices() {
        List<Device> list = new ArrayList<>();
        for (int i = 0; i < _checks.length; i++) {
            if (_checks[i]) {
                list.add(_devices[i]);
            }
        }
        return list;
    }

    private static class CreateDatapointHandler extends Handler {
        private WeakReference<Device> _device;
        private AylaProperty _property;
        private View _view;
        private SceneDeviceSelectionAdapter _listener;

        public CreateDatapointHandler(Device device, AylaProperty property, View view, SceneDeviceSelectionAdapter listener) {
            _device = new WeakReference<Device>(device);
            _property = property;
            _view = view;
            _listener = listener;
        }

        @Override
        public void handleMessage(Message msg) {
            Logger.logMessage(LOG_TAG, msg, "zs: toggleValue");
            SessionManager.deviceManager().statusUpdated(_device.get(), false);
            if ( _listener != null ) {
                _listener.updateView(_view);
            }
        }
    }

    void updateView(View view) {
        Switch sv = (Switch)view.findViewById(R.id.device_property);
        Device device = (Device)sv.getTag();
        AylaProperty prop = device.getProperty(device.getObservablePropertyName());
        if (prop != null) {
            sv.setVisibility(View.VISIBLE);
            boolean value = false;
            try {
                value = (prop.value != null && Integer.parseInt(prop.value) != 0);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            sv.setChecked(value);
        } else {
            sv.setVisibility(View.GONE);
        }
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final View view = super.getView(position, convertView, parent);

        CheckBox cv = (CheckBox)view.findViewById(R.id.device_selected);
        TextView tv = (TextView)view.findViewById(R.id.device_name);
        Switch sv = (Switch)view.findViewById(R.id.device_property);

        Device device = getItem(position);
        view.setTag(device);
        cv.setTag(device);
        tv.setTag(device);
        sv.setTag(device);

        cv.setChecked(_checks[position]);
        cv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _checks[position] = ((CheckBox)v).isChecked();
            }
        });
        tv.setText(device.getProductName());

        AylaProperty prop = device.getProperty(device.getObservablePropertyName());
        if (prop != null) {
            sv.setVisibility(View.VISIBLE);
            boolean value = false;
            try {
                value = (prop.value != null && Integer.parseInt(prop.value) != 0);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            sv.setChecked(value);
            sv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Device device = (Device)v.getTag();
                    AylaProperty prop = device.getProperty(device.getObservablePropertyName());
                    Boolean newValue = "0".equals(prop.value);
                    AylaDatapoint datapoint = new AylaDatapoint();
                    datapoint.nValue(newValue ? 1 : 0);
                    prop.value = newValue ? "1" : "0";
                    final CreateDatapointHandler handler = new CreateDatapointHandler(device, prop, view, SceneDeviceSelectionAdapter.this);
                    prop.createDatapoint(handler, datapoint);
                }
            });
        } else {
            sv.setVisibility(View.GONE);
        }

        return view;
    }
}