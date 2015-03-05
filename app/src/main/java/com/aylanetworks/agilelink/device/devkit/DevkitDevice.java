package com.aylanetworks.agilelink.device.devkit;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.aylanetworks.aaml.AylaDatapoint;
import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaProperty;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.SessionManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/*
 * DevkitDevice.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 1/14/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class DevkitDevice extends Device implements View.OnClickListener {
    private static final String LOG_TAG = "DevkitDevice";

    private static final String PROPERTY_BLUE_LED = "Blue_LED";
    private static final String PROPERTY_GREEN_LED = "Green_LED";
    private static final String PROPERTY_BLUE_BUTTON = "Blue_button";

    public DevkitDevice(AylaDevice device) {
        super(device);
    }

    @Override
    public String deviceTypeName() {
        return "Ayla EVB";
    }

    public boolean isBlueButtonPressed() {
        AylaProperty prop = getProperty(PROPERTY_BLUE_BUTTON);
        if (prop != null && prop.value != null && Integer.parseInt(prop.value) != 0) {
            return true;
        }
        return false;
    }

    static class CreateDatapointHandler extends Handler {
        private WeakReference<DevkitDevice> _devkitDevice;
        private String _propertyName;
        public CreateDatapointHandler(DevkitDevice devkitDevice, String propertyName) {
            _devkitDevice = new WeakReference<DevkitDevice>(devkitDevice);
            _propertyName = propertyName;
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.i(LOG_TAG, "Devkit: createDatapointHandler called: " + msg);
            if ( msg.what == AylaNetworks.AML_ERROR_OK ) {
                // Set the value of the property
                AylaDatapoint dp = AylaSystemUtils.gson.fromJson((String)msg.obj, AylaDatapoint.class);
                for ( int i = 0; i < _devkitDevice.get().getDevice().properties.length; i++ ) {
                    AylaProperty p = _devkitDevice.get().getDevice().properties[i];
                    if ( p.name.equals(_propertyName)) {
                        p.datapoint = dp;
                        p.value = dp.value();
                        break;
                    }
                }
            }
            SessionManager.deviceManager().notifyDeviceStatusChanged(_devkitDevice.get());
        }
    }

    public void setGreenLED(boolean on) {
        AylaProperty greenLED = getProperty(PROPERTY_GREEN_LED);
        if (greenLED == null) {
            Log.e(LOG_TAG, "Couldn't find property: " + PROPERTY_GREEN_LED);
            SessionManager.deviceManager().refreshDeviceStatus(this);
            return;
        }

        AylaDatapoint dp = new AylaDatapoint();
        dp.nValue(on ? 1 : 0);
        greenLED.createDatapoint(new CreateDatapointHandler(this, PROPERTY_GREEN_LED), dp);
    }

    public void setBlueLED(boolean on) {
        AylaProperty blueLED = getProperty(PROPERTY_BLUE_LED);
        if (blueLED == null) {
            Log.e(LOG_TAG, "Couldn't find property: " + PROPERTY_GREEN_LED);
            SessionManager.deviceManager().refreshDeviceStatus(this);
            return;
        }

        AylaDatapoint dp = new AylaDatapoint();
        dp.nValue(on ? 1 : 0);
        blueLED.createDatapoint(new CreateDatapointHandler(this, PROPERTY_BLUE_LED), dp);
    }

    public boolean isGreenLEDOn() {
        AylaProperty prop = getProperty(PROPERTY_GREEN_LED);
        if (prop != null && prop.value != null && Integer.parseInt(prop.value) != 0) {
            return true;
        }
        return false;
    }

    public boolean isBlueLEDOn() {
        AylaProperty prop = getProperty(PROPERTY_BLUE_LED);
        if (prop != null && prop.value != null && Integer.parseInt(prop.value) != 0) {
            return true;
        }
        return false;
    }

    @Override
    public String[] getSchedulablePropertyNames() {
        return new String[]{PROPERTY_BLUE_LED, PROPERTY_GREEN_LED};
    }

    @Override
    public String friendlyNameForPropertyName(String propertyName) {
        switch ( propertyName ) {
            case PROPERTY_BLUE_LED:
                return MainActivity.getInstance().getString(R.string.blue_led);

            case PROPERTY_GREEN_LED:
                return MainActivity.getInstance().getString(R.string.green_led);
        }
        return MainActivity.getInstance().getString(R.string.unknown_property_name);
    }

    @Override
    protected ArrayList<String> getPropertyNames() {
        ArrayList<String> propertyNames = super.getPropertyNames();

        // Add property names we care about
        propertyNames.add(PROPERTY_BLUE_BUTTON);
        propertyNames.add(PROPERTY_BLUE_LED);
        propertyNames.add(PROPERTY_GREEN_LED);

        return propertyNames;
    }


    @Override
    public Drawable getDeviceDrawable(Context c) {
        return c.getResources().getDrawable(R.drawable.evb);
    }

    @Override
    public void onClick(View v) {
        // The green or blue LED has been tapped.
        boolean isGreenButton = (v.getId() == R.id.green_button);

        Log.i(LOG_TAG, "Button tapped: " + (isGreenButton ? "GREEN" : "BLUE"));
        if (isGreenButton) {
            setGreenLED(!isGreenLEDOn());
        } else {
            setBlueLED(!isBlueLEDOn());
        }

        // Update the image view to show the transient state
        ImageButton button = (ImageButton) v;
        button.setImageDrawable(v.getContext().getResources().getDrawable(R.drawable.dpending));
    }

    public static final int ITEM_VIEW_TYPE_DEVKIT_DEVICE = 1;

    @Override
    public int getItemViewType() {
        return ITEM_VIEW_TYPE_DEVKIT_DEVICE;
    }

    @Override
    public void bindViewHolder(RecyclerView.ViewHolder holder) {
        // Device name
        DevkitDeviceViewHolder h = (DevkitDeviceViewHolder) holder;
        h._deviceNameTextView.setText(getDevice().getProductName());

        // Blue button state
        int imageId = isBlueButtonPressed() ? R.drawable.buttondown : R.drawable.buttonup;
        h._buttonStateImageView.setImageDrawable(h._buttonStateImageView.getContext().getResources().getDrawable(imageId));

        // Green LED state + button
        int bulbId = isGreenLEDOn() ? R.drawable.dup : R.drawable.ddown;
        h._greenButton.setImageDrawable(h._greenButton.getContext().getResources().getDrawable(bulbId));
        h._greenButton.setOnClickListener(this);

        // Blue LED state + button
        bulbId = isBlueLEDOn() ? R.drawable.dup : R.drawable.ddown;
        h._blueButton.setImageDrawable(h._blueButton.getContext().getResources().getDrawable(bulbId));
        h._blueButton.setOnClickListener(this);
    }
}
