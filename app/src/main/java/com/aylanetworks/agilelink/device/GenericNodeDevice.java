package com.aylanetworks.agilelink.device;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import com.aylanetworks.aaml.AylaDatapoint;
import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaProperty;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;

import java.util.ArrayList;

/*
 * GenericNodeDevice.java
 * AgileLink Application Framework
 *
 * Created by David Junod on 7/9/15
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class GenericNodeDevice extends GenericDevice implements View.OnClickListener{

    private final static String LOG_TAG = GenericNodeDevice.class.getSimpleName();


    private static final String PROPERTY_OFF_CMD = "01:0006_S:00";
    private static final String PROPERTY_ON_CMD = "01:0006_S:01";
    private static final String PROPERTY_ONOFF_STATUS = "01:0006_S:0000";

    public GenericNodeDevice(AylaDevice device) {
        super(device);
    }

    @Override
    public String friendlyNameForPropertyName(String propertyName) {
        AylaProperty ap = getProperty(propertyName);
        if (ap == null || TextUtils.isEmpty(ap.displayName)) {
            return super.friendlyNameForPropertyName(propertyName);
        }
        return ap.displayName;
    }

    @Override
    public int getItemViewType() {
        return AgileLinkDeviceCreator.ITEM_VIEW_TYPE_GENERIC_NODE_DEVICE;
    }

    @Override
    public boolean isDeviceNode() {
        return true;
    }

    @Override
    public String registrationType() {
        return AylaNetworks.AML_REGISTRATION_TYPE_NODE;
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


    @Override
    public void bindViewHolder(RecyclerView.ViewHolder holder) {
        super.bindViewHolder(holder);

        GenericNodeDeviceViewHolder h = (GenericNodeDeviceViewHolder)holder;
        h._deviceNameTextView.setText(getProductName());
        h._switchButton.setOnClickListener(this);

        Resources res = MainActivity.getInstance().getResources();
        int imgID = isNodeDeviceOn()? R.drawable.ic_power_on : R.drawable.ic_power_off;
        h._switchButton.setImageDrawable(res.getDrawable(imgID));
    }// end of bindViewHolder

    @Override
    protected ArrayList<String> getPropertyNames() {
        ArrayList<String> pNames = super.getPropertyNames();

        pNames.add(PROPERTY_OFF_CMD);
        pNames.add(PROPERTY_ON_CMD);
        pNames.add(PROPERTY_OFF_CMD);

        return pNames;
    }// end of getPropertyNames

    @Override
    public void onClick(View v) {
        if (isNodeDeviceOn()) {
            setOff();
        } else {
            setOn();
        }
        // Update the image view to show the transient state
        ImageButton button = (ImageButton) v;
        button.setImageDrawable(v.getContext().getResources().getDrawable(R.drawable.dpending));
    }// end of onClick

    private boolean isNodeDeviceOn() {
        AylaProperty prop = getProperty(PROPERTY_ONOFF_STATUS);
        if (prop!=null && prop.value !=null && Integer.parseInt(prop.value) != 0) {
            return true;
        }
        return false;
    }

    private void setOn() {
        setDatapoint(PROPERTY_ON_CMD, 1, new SetDatapointListener(){
            @Override
            public void  setDatapointComplete(boolean succeeded, AylaDatapoint newDatapoint) {
                Log.d(LOG_TAG, "setON: " + succeeded + " ***^^^");
            }
        });
    }

    private void setOff() {
        setDatapoint(PROPERTY_OFF_CMD, 1, new SetDatapointListener(){
            @Override
            public void  setDatapointComplete(boolean succeeded, AylaDatapoint newDatapoint) {
                Log.d(LOG_TAG, "setOFF: " + succeeded + " ***^^^");
            }
        });
    }
}// end of GenericNodeDevice class
