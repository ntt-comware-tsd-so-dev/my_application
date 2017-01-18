package com.aylanetworks.agilelink.device;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.aylanetworks.agilelink.AgileLinkApplication;
import com.aylanetworks.agilelink.ErrorUtils;
import com.aylanetworks.agilelink.framework.AMAPCore;
import com.aylanetworks.aylasdk.AylaDatapoint;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaProperty;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.aylasdk.error.AylaError;

import java.util.ArrayList;

/*
 * AylaEVBDevice.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 1/14/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class AylaEVBDevice extends GenericDevice implements View.OnClickListener {

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
        AylaProperty prop = _device.getProperty(PROPERTY_BLUE_BUTTON);
        if (prop != null && prop.getValue() != null && (Integer)prop.getValue() != 0) {
            return true;
        }
        return false;
    }


    public void setGreenLED(final boolean on, final ImageView button) {
       setDatapoint(PROPERTY_GREEN_LED, on ? 1 : 0, new SetDatapointListener() {
           @Override
           public void setDatapointComplete(AylaDatapoint newDatapoint, AylaError error) {
               button.setImageDrawable(button.getContext().getResources().getDrawable
                       (isGreenLEDOn() ? R.drawable.green_led_on : R.drawable.green_led_off));

               if (error != null) {
                   Toast.makeText(button.getContext(),
                           ErrorUtils.getUserMessage(error, "Error setting green LED state"), //TODO: Localize message in R.string
                           Toast.LENGTH_SHORT).show();
               }
           }
       });
    }

    public void setBlueLED(final boolean on, final ImageView button) {
        setDatapoint(PROPERTY_BLUE_LED, on ? 1 : 0, new SetDatapointListener() {
            @Override
            public void setDatapointComplete(AylaDatapoint newDatapoint, AylaError error) {
                button.setImageDrawable(button.getContext().getResources().getDrawable
                        (isBlueLEDOn() ? R.drawable.blue_led_on : R.drawable.blue_led_off));

                if (error != null) {
                    Toast.makeText(button.getContext(),
                            ErrorUtils.getUserMessage(error, "Error setting blue LED state"), //TODO: Localize message in R.string
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public boolean isGreenLEDOn() {
        AylaProperty prop = _device.getProperty(PROPERTY_GREEN_LED);
        if (prop != null && prop.getValue() != null && (int) prop.getValue() != 0) {
            return true;
        }
        return false;
    }

    public boolean isBlueLEDOn() {
        AylaProperty prop = _device.getProperty(PROPERTY_BLUE_LED);
        if (prop != null && prop.getValue() != null && (int) prop.getValue() != 0) {
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
                return AgileLinkApplication.getAppContext().getString(R.string.blue_led);

            case PROPERTY_GREEN_LED:
                return AgileLinkApplication.getAppContext().getString(R.string.green_led);

            case PROPERTY_BLUE_BUTTON:
                return AgileLinkApplication.getAppContext().getString(R.string.blue_button);
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
        Log.d(LOG_TAG, "lm: Button tapped: " + (isGreenButton ? "GREEN" : "BLUE") + " ***vvv");
        if(isOnline() || isInLanMode()){
            ImageButton button = (ImageButton) v;

            if (isGreenButton) {
                setGreenLED(!isGreenLEDOn(), button);
            } else {
                setBlueLED(!isBlueLEDOn(), button);
            }

            // Update the image view to show the transient state
            /*button.setImageDrawable(v.getContext().getResources().getDrawable(R.drawable
                    .dpending));*/
        } else {
            Toast.makeText(MainActivity.getInstance(), R.string.offline_no_functionality,
                    Toast.LENGTH_SHORT).show();
        }
    }

    public static final int ITEM_VIEW_TYPE_DEVKIT_DEVICE = 1;

    @Override
    public int getItemViewType() {
        return ITEM_VIEW_TYPE_DEVKIT_DEVICE;
    }

    @Override
    public void bindViewHolder(RecyclerView.ViewHolder holder) {
        // Device name
        super.bindViewHolder(holder);

        AylaEVBDeviceViewHolder h = (AylaEVBDeviceViewHolder) holder;
        Resources res = MainActivity.getInstance().getResources();
        
        h._deviceNameTextView.setText(_device.getProductName());

        // Blue button state
        int imageId = isBlueButtonPressed() ? R.drawable.buttondown : R.drawable.buttonup;
        h._buttonStateImageView.setImageDrawable(res.getDrawable(imageId));

        // Green LED state + button
        int bulbId = isGreenLEDOn() ? R.drawable.green_led_on : R.drawable.green_led_off;
        h._greenButton.setImageDrawable(res.getDrawable(bulbId));
        h._greenButton.setOnClickListener(this);

        // Blue LED state + button
        bulbId = isBlueLEDOn() ? R.drawable.blue_led_on : R.drawable.blue_led_off;
        h._blueButton.setImageDrawable(res.getDrawable(bulbId));
        h._blueButton.setOnClickListener(this);
    }

    @Override
    public int getGridViewSpan() {
        return 1;
    }
}
