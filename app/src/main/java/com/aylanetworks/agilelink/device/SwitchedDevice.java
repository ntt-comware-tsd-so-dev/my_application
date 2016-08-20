package com.aylanetworks.agilelink.device;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.aylanetworks.aylasdk.AylaDatapoint;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaProperty;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.aylasdk.error.AylaError;

import java.util.ArrayList;

/*
 * SwitchedDevice.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 2/4/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class SwitchedDevice extends GenericDevice implements View.OnClickListener {

    private final static String LOG_TAG = "SwitchedDevice";
    private final static String PROPERTY_OUTLET = "outlet1";

    public SwitchedDevice(AylaDevice device) {
        super(device);
    }

    public void toggle() {
        if (isOnline() || isInLanMode()) {
            AylaProperty prop = getProperty(getObservablePropertyName());
            if (prop == null) {
                Log.e(LOG_TAG, "Could not find property " + getObservablePropertyName());
                return;
            }

            // Get the opposite value and set it
            Integer newValue = ((Integer) prop.getValue() == 0) ? 1 : 0;
            setDatapoint(getObservablePropertyName(), newValue, new SetDatapointListener() {
                @Override
                public void setDatapointComplete(AylaDatapoint newDatapoint, AylaError error) {
                    Log.d(LOG_TAG, "lm: setSwitch error: " + error + " ***^^^");
                }
            });
        }
    }

    public boolean isOn() {
        AylaProperty prop = getProperty(getObservablePropertyName());
        if (prop != null && prop.getValue() != null && (Integer) prop.getValue() != 0) {
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

    public Drawable getSwitchedDrawable(Resources res, boolean isOn) {
        return res.getDrawable(isOn ? R.drawable.ic_power_on : R.drawable.ic_power_off);
    }

    protected Drawable getSwitchedDrawable(Resources res) {
        return getSwitchedDrawable(res, isOn());
    }

    public Drawable getSwitchedPendingDrawable(Resources res) {
        return res.getDrawable(R.drawable.ic_power_pending);
    }

    @Override
    public AylaDevice.RegistrationType registrationType() {
        return AylaDevice.RegistrationType.ButtonPush;
    }

    @Override
    public int getItemViewType() {
        return AMAPViewModelProvider.ITEM_VIEW_TYPE_SWITCHED;
    }

    @Override
    public void bindViewHolder(RecyclerView.ViewHolder holder) {
        super.bindViewHolder(holder);

        if (holder instanceof SwitchedDeviceViewHolder) {
            Resources res = MainActivity.getInstance().getResources();
            Drawable buttonDrawable = getSwitchedDrawable(res);
            SwitchedDeviceViewHolder h = (SwitchedDeviceViewHolder) holder;
            h._switchButton.setImageDrawable(buttonDrawable);
            h._switchButton.setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        if (isOnline() || isInLanMode()) {
            // Toggle the button state
            Log.d(LOG_TAG, "lm: Switch tapped ***vvv");
            ImageButton button = (ImageButton) v;
            button.setImageDrawable(getSwitchedPendingDrawable(v.getResources()));
            toggle();
        } else {
            Toast.makeText(MainActivity.getInstance(), R.string.offline_no_functionality,
                    Toast.LENGTH_SHORT).show();
        }
    }
}
