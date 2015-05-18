package com.aylanetworks.agilelink.device;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.aylanetworks.aaml.AylaDatapoint;
import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.aaml.AylaProperty;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.Device;

import java.util.ArrayList;

/*
 * AylaEVBDevice.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 1/14/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class AylaEVBDevice extends Device implements View.OnClickListener {
    private static final String LOG_TAG = "AylaEVBDevice";

    private static final String PROPERTY_BLUE_LED = "Blue_LED";
    private static final String PROPERTY_GREEN_LED = "Green_LED";
    private static final String PROPERTY_BLUE_BUTTON = "Blue_button";

    public AylaEVBDevice(AylaDevice device) {
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


    public void setGreenLED(boolean on) {
       setDatapoint(PROPERTY_GREEN_LED, on, new SetDatapointListener() {
           @Override
           public void setDatapointComplete(boolean succeeded, AylaDatapoint newDatapoint) {
               Log.d(LOG_TAG, "setGreenLED: " + succeeded);
           }
       });
    }

    public void setBlueLED(boolean on) {
        setDatapoint(PROPERTY_BLUE_LED, on, new SetDatapointListener() {
            @Override
            public void setDatapointComplete(boolean succeeded, AylaDatapoint newDatapoint) {
                Log.d(LOG_TAG, "setGreenLED: " + succeeded);
            }
        });
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
    public String[] getNotifiablePropertyNames() {
        return new String[]{PROPERTY_BLUE_BUTTON, PROPERTY_BLUE_LED, PROPERTY_GREEN_LED};
    }

    @Override
    public String friendlyNameForPropertyName(String propertyName) {
        switch (propertyName) {
            case PROPERTY_BLUE_LED:
                return MainActivity.getInstance().getString(R.string.blue_led);

            case PROPERTY_GREEN_LED:
                return MainActivity.getInstance().getString(R.string.green_led);

            case PROPERTY_BLUE_BUTTON:
                return MainActivity.getInstance().getString(R.string.blue_button);
        }
        return super.friendlyNameForPropertyName(propertyName);
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

        if ( !isOnline() ) {
            Toast.makeText(MainActivity.getInstance(), R.string.offline_no_functionality, Toast.LENGTH_LONG).show();
            return;
        }

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
        AylaEVBDeviceViewHolder h = (AylaEVBDeviceViewHolder) holder;
        Resources res = MainActivity.getInstance().getResources();

        h._spinner.setVisibility(getDevice().properties == null ? View.VISIBLE : View.GONE);

        h._deviceNameTextView.setText(getDevice().getProductName());

        // Blue button state
        int imageId = isBlueButtonPressed() ? R.drawable.buttondown : R.drawable.buttonup;
        h._buttonStateImageView.setImageDrawable(res.getDrawable(imageId));

        // Green LED state + button
        int bulbId = isGreenLEDOn() ? R.drawable.dup : R.drawable.ddown;
        h._greenButton.setImageDrawable(res.getDrawable(bulbId));
        h._greenButton.setOnClickListener(this);

        // Blue LED state + button
        bulbId = isBlueLEDOn() ? R.drawable.dup : R.drawable.ddown;
        h._blueButton.setImageDrawable(res.getDrawable(bulbId));
        h._blueButton.setOnClickListener(this);

        // Is this a shared device?
        int color = isOnline() ? res.getColor(R.color.card_text) : res.getColor(R.color.disabled_text);
        if (!getDevice().amOwner()) {
            // Yes, this device is shared.
            color = res.getColor(R.color.card_shared_text);
        }
        h._deviceNameTextView.setTextColor(color);
    }

    @Override
    public int getGridViewSpan() {
        return 1;
    }
}