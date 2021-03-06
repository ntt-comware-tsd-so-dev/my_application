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

/**
 * AMAP4.x
 * <p/>
 * Copyright 2016 Ayla Networks Inc, all rights reserved
 */
public class GenericSwitchedDevice extends GenericNodeDevice implements View.OnClickListener {
    private final static String LOG_TAG = GenericSwitchedDevice.class.getSimpleName();

    private static final String PROPERTY_OFF_CMD = "01:0006_S:00";
    private static final String PROPERTY_ON_CMD = "01:0006_S:01";
    private static final String PROPERTY_ONOFF_STATUS = "01:0006_S:0000";

    public GenericSwitchedDevice(AylaDevice device) {
        super(device);
    }


    @Override
    public int getItemViewType() {
        return AMAPViewModelProvider.ITEM_VIEW_TYPE_SWITCHED;
    }

    @Override
    public String deviceTypeName() {
        return "Generic Node";
    }

    @Override
    public Drawable getDeviceDrawable(Context c) {
        return c.getResources().getDrawable(R.drawable.ic_generic_node_red);
    }

    @Override
    public String[] getSchedulablePropertyNames() {
        return new String[]{PROPERTY_ON_CMD, PROPERTY_OFF_CMD};
    }

    @Override
    public String[] getNotifiablePropertyNames() {
        return new String[]{PROPERTY_ONOFF_STATUS};
    }

    @Override
    public int getGridViewSpan() {
        return 1;
    }

    public Drawable getSwitchedDrawable(Resources res, boolean isOn) {
        return res.getDrawable(isOn ? R.drawable.ic_power_on : R.drawable.ic_power_off);
    }

    protected Drawable getSwitchedDrawable(Resources res) {
        return getSwitchedDrawable(res, isDeviceOn());
    }

    public Drawable getSwitchedPendingDrawable(Resources res) {
        return res.getDrawable(R.drawable.ic_power_pending);
    }

    @Override
    public void bindViewHolder(RecyclerView.ViewHolder holder) {
        super.bindViewHolder(holder);

        SwitchedDeviceViewHolder h = (SwitchedDeviceViewHolder)holder;
        h._deviceNameTextView.setText(getDevice().getProductName());
        h._switchButton.setOnClickListener(this);

        Resources res = MainActivity.getInstance().getResources();
        h._switchButton.setImageDrawable(getSwitchedDrawable(res));
    }// end of bindViewHolder

    @Override
    protected ArrayList<String> getPropertyNames() {
        ArrayList<String> pNames = super.getPropertyNames();

        pNames.add(PROPERTY_OFF_CMD);
        pNames.add(PROPERTY_ON_CMD);
        pNames.add(PROPERTY_ONOFF_STATUS);

        return pNames;
    }// end of getPropertyNames

    @Override
    public void onClick(View v) {
        // Update the image view to show the transient state
        if (isOnline() || isInLanMode()) {
            ImageButton button = (ImageButton) v;
            button.setImageDrawable(getSwitchedPendingDrawable(v.getResources()));

            if (isDeviceOn()) {
                setOff();
            } else {
                setOn();
            }
        } else {
            Toast.makeText(MainActivity.getInstance(), R.string.offline_no_functionality,
                    Toast.LENGTH_SHORT).show();
        }
    }// end of onClick

    private boolean isDeviceOn() {
        AylaProperty prop = getProperty(PROPERTY_ONOFF_STATUS);
        if (prop != null && prop.getValue() != null && (Integer)prop.getValue() != 0) {
            return true;
        }
        return false;
    }

    private void setOn() {
        setDatapoint(PROPERTY_ON_CMD, 1, new SetDatapointListener(){
            @Override
            public void  setDatapointComplete(AylaDatapoint newDatapoint, AylaError error) {
                Log.d(LOG_TAG, "setON error: " + error + " ***^^^");
            }
        });
    }

    private void setOff() {
        setDatapoint(PROPERTY_OFF_CMD, 1, new SetDatapointListener(){
            @Override
            public void  setDatapointComplete(AylaDatapoint newDatapoint, AylaError error) {
                Log.d(LOG_TAG, "setOFF error: " + error + " ***^^^");
            }
        });
    }
}// end of GenericSwitchedDevice class




