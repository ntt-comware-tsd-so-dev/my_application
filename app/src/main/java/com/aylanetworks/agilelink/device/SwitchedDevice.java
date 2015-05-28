package com.aylanetworks.agilelink.device;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaProperty;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.SessionManager;

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
    private final static String PROPERTY_OUTLET = "outlet1";

    public SwitchedDevice(AylaDevice device) {
        super(device);
    }

    public void toggle() {
        AylaProperty prop = getProperty(getObservablePropertyName());
        if (prop == null) {
            Log.e(LOG_TAG, "Could not find property " + getObservablePropertyName());
            SessionManager.deviceManager().refreshDeviceStatus(this);
            return;
        }

        // Get the opposite boolean value and set it
        Boolean newValue = "0".equals(prop.value);
        setDatapoint(getObservablePropertyName(), newValue, null);
    }

    public boolean isOn() {
        AylaProperty prop = getProperty(getObservablePropertyName());
        if (prop != null && prop.value != null && Integer.parseInt(prop.value) != 0) {
            return true;
        }

        return false;
    }

    @Override
    public String[] getSchedulablePropertyNames() {
        return new String[]{getObservablePropertyName()};
    }

    @Override
    public String[] getNotifiablePropertyNames() {
        return new String[]{getObservablePropertyName()};
    }

    @Override
    public String friendlyNameForPropertyName(String propertyName) {
        if (propertyName.equals(getObservablePropertyName())) {
            return MainActivity.getInstance().getString(R.string.property_outlet_friendly_name);
        }
        return super.friendlyNameForPropertyName(propertyName);
    }

    @Override
    protected ArrayList<String> getPropertyNames() {
        ArrayList<String> list = super.getPropertyNames();
        list.add(getObservablePropertyName());
        return list;
    }

    @Override
    public String getObservablePropertyName() { return PROPERTY_OUTLET;  }

    @Override
    public String deviceTypeName() {
        return "Smart Plug";
    }

    @Override
    public Drawable getDeviceDrawable(Context c) {
        return c.getResources().getDrawable(R.drawable.smart_plug);
    }

    public Drawable getSwitchedDrawable(Resources res) {
        return res.getDrawable(isOn() ? R.drawable.ic_power_on : R.drawable.ic_power_off);
    }

    public Drawable getSwitchedPendingDrawable(Resources res) {
        return res.getDrawable(R.drawable.ic_power_pending);
    }

    @Override
    public String registrationType() {
        return AylaNetworks.AML_REGISTRATION_TYPE_BUTTON_PUSH;
    }

    @Override
    public int getItemViewType() {
        return AgileLinkDeviceCreator.ITEM_VIEW_TYPE_SWITCHED;
    }

    @Override
    public void bindViewHolder(RecyclerView.ViewHolder holder) {
        Resources res = MainActivity.getInstance().getResources();

        SwitchedDeviceViewHolder h = (SwitchedDeviceViewHolder) holder;
        h._spinner.setVisibility(getDevice().properties == null ? View.VISIBLE : View.GONE);
        h._deviceNameTextView.setText(getDevice().getProductName());

        Drawable buttonDrawable = getSwitchedDrawable(res);

        h._switchButton.setImageDrawable(buttonDrawable);
        h._switchButton.setOnClickListener(this);

        // Is this a shared device?
        int color = res.getColor(R.color.card_text);
        if (!getDevice().amOwner()) {
            // Yes, this device is shared.
            color = res.getColor(R.color.card_shared_text);
        }
        if (!isOnline() ) {
            color = res.getColor(R.color.disabled_text);
        }

        h._deviceNameTextView.setTextColor(color);
    }


    @Override
    public void onClick(View v) {
        if ( !isOnline() ) {
            Toast.makeText(MainActivity.getInstance(), R.string.offline_no_functionality, Toast.LENGTH_LONG).show();
            return;
        }
        // Toggle the button state
        ImageButton button = (ImageButton) v;
        button.setImageDrawable(getSwitchedPendingDrawable(v.getResources()));
        toggle();
    }
}
