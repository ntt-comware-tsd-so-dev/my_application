package com.aylanetworks.agilelink.device.devkit;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import com.aylanetworks.aaml.AylaDatapoint;
import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaProperty;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.DeviceManager;
import com.aylanetworks.agilelink.framework.SessionManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/*
 * SwitchedDevice.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 2/4/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class SwitchedDevice extends Device implements View.OnClickListener {
    private final static String LOG_TAG = "SwitchedDevice";
    public final static String PROPERTY_OUTLET = "outlet1";

    public SwitchedDevice(AylaDevice device) {
        super(device);
    }

    public void toggle() {
        AylaProperty prop = getProperty(PROPERTY_OUTLET);
        if (prop == null) {
            Log.e(LOG_TAG, "Could not find property " + PROPERTY_OUTLET);
            SessionManager.deviceManager().refreshDeviceStatus(this);
            return;
        }

        // Get the opposite boolean value and set it
        Boolean newValue = "0".equals(prop.value);
        setDatapoint(PROPERTY_OUTLET, newValue, null);
    }

    public boolean isOn() {
        AylaProperty prop = getProperty(PROPERTY_OUTLET);
        if (prop != null && prop.value != null && Integer.parseInt(prop.value) != 0) {
            return true;
        }

        return false;
    }

    @Override
    public String[] getSchedulablePropertyNames() {
        return new String[]{PROPERTY_OUTLET};
    }

    @Override
    public String friendlyNameForPropertyName(String propertyName) {
        if (propertyName.equals(PROPERTY_OUTLET)) {
            return MainActivity.getInstance().getString(R.string.property_outlet_friendly_name);
        }
        return MainActivity.getInstance().getString(R.string.unknown_property_name);
    }

    @Override
    protected ArrayList<String> getPropertyNames() {
        ArrayList<String> list = super.getPropertyNames();
        list.add(PROPERTY_OUTLET);

        return list;
    }

    @Override
    public String deviceTypeName() {
        return "Smart Plug";
    }

    @Override
    public Drawable getDeviceDrawable(Context c) {
        return c.getResources().getDrawable(R.drawable.smart_plug);
    }

    @Override
    public String registrationType() {
        return AylaNetworks.AML_REGISTRATION_TYPE_BUTTON_PUSH;
    }

    @Override
    public int getItemViewType() {
        return AgileLinkDeviceCreator.ITEM_VIEW_TYPE_SMARTPLUG;
    }

    @Override
    public void bindViewHolder(RecyclerView.ViewHolder holder) {
        SwitchedDeviceViewHolder h = (SwitchedDeviceViewHolder) holder;
        h._spinner.setVisibility(getDevice().properties == null ? View.VISIBLE : View.GONE);
        h._deviceNameTextView.setText(getDevice().getProductName());

        int drawableId = isOn() ? R.drawable.smartplug_button_on : R.drawable.smartplug_button_off;
        Drawable buttonDrawable = h._switchButton.getContext().getResources().getDrawable(drawableId);

        h._switchButton.setImageDrawable(buttonDrawable);
        h._switchButton.setOnClickListener(this);

        // Is this a shared device?
        int color = MainActivity.getInstance().getResources().getColor(R.color.card_text);
        if (!getDevice().amOwner()) {
            // Yes, this device is shared.
            color = MainActivity.getInstance().getResources().getColor(R.color.card_shared_text);
        }
        h._deviceNameTextView.setTextColor(color);
    }


    @Override
    public void onClick(View v) {
        // Toggle the button state
        ImageButton button = (ImageButton) v;
        button.setImageDrawable(v.getResources().getDrawable(R.drawable.smartplug_button_pending));
        toggle();
    }
}
