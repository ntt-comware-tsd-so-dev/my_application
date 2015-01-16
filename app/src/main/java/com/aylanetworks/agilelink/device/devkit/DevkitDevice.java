package com.aylanetworks.agilelink.device.devkit;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
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
import com.aylanetworks.aaml.AylaProperty;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.SessionManager;

import java.util.ArrayList;

/**
 * Created by Brian King on 1/14/15.
 */
public class DevkitDevice extends Device implements View.OnClickListener {
    private static final String LOG_TAG = "DevkitDevice";

    private static final String PROPERTY_BLUE_LED = "Blue_LED";
    private static final String PROPERTY_GREEN_LED = "Green_LED";
    private static final String PROPERTY_BLUE_BUTTON = "Blue_button";

    public DevkitDevice(AylaDevice device) {
        super(device);
    }

    public boolean isBlueButtonPressed() {
        AylaProperty prop = getProperty(PROPERTY_BLUE_BUTTON);
        if ( prop != null && prop.value != null && Integer.parseInt(prop.value) != 0 ) {
            return true;
        }
        return false;
    }

    private Handler _createDatapointHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.i(LOG_TAG, "Devkit: createDatapointHandler called: " + msg);

            // Let the device manager know that we've updated ourselves.
            SessionManager.deviceManager().refreshDeviceStatus(DevkitDevice.this);
        }
    };

    public void setGreenLED(boolean on) {
        AylaProperty greenLED = getProperty(PROPERTY_GREEN_LED);
        if ( greenLED == null ) {
            Log.e(LOG_TAG, "Couldn't find property: " + PROPERTY_GREEN_LED);
            return;
        }

        AylaDatapoint dp = new AylaDatapoint();
        dp.nValue(on ? 1 : 0);
        try {
            greenLED.createDatapoint(_createDatapointHandler, dp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setBlueLED(boolean on) {
        AylaProperty blueLED = getProperty(PROPERTY_BLUE_LED);
        if ( blueLED == null ) {
            Log.e(LOG_TAG, "Couldn't find property: " + PROPERTY_GREEN_LED);
            return;
        }

        AylaDatapoint dp = new AylaDatapoint();
        dp.nValue(on ? 1 : 0);
        try {
            blueLED.createDatapoint(_createDatapointHandler, dp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isGreenLEDOn() {
        AylaProperty prop = getProperty(PROPERTY_GREEN_LED);
        if ( prop != null && prop.value != null && Integer.parseInt(prop.value) != 0 ) {
            return true;
        }
        return false;
    }

    public boolean isBlueLEDOn() {
        AylaProperty prop = getProperty(PROPERTY_BLUE_LED);
        if ( prop != null && prop.value != null && Integer.parseInt(prop.value) != 0 ) {
            return true;
        }
        return false;
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
    public View getListItemView(Context c, View convertView, ViewGroup parent) {
        if ( convertView == null || convertView.findViewById(R.id.blue_button) == null ) {
            convertView = LayoutInflater.from(c).inflate(R.layout.devkit_list_item, parent, false);
        }

        convertView.setBackgroundColor(c.getResources().getColor(R.color.list_background));

        TextView deviceNameTextView = (TextView)convertView.findViewById(R.id.device_name);
        ImageButton greenButton = (ImageButton)convertView.findViewById(R.id.green_button);
        ImageButton blueButton = (ImageButton)convertView.findViewById(R.id.blue_button);
        ImageView buttonStateImageView = (ImageView)convertView.findViewById(R.id.blue_button_state_image);

        greenButton.setOnClickListener(this);
        blueButton.setOnClickListener(this);

        greenButton.setImageDrawable(c.getResources().getDrawable((isGreenLEDOn() ?
                R.drawable.dup : R.drawable.ddown)));
        blueButton.setImageDrawable(c.getResources().getDrawable((isBlueLEDOn() ?
                R.drawable.dup : R.drawable.ddown)));

        buttonStateImageView.setImageDrawable(c.getResources().getDrawable((isBlueButtonPressed() ?
                R.drawable.downblue : R.drawable.buttonup)));

        deviceNameTextView.setText(toString());

        return convertView;
    }

    @Override
    public Drawable getDeviceDrawable(Context c) {
        return c.getResources().getDrawable(R.drawable.evb);
    }

    @Override
    public String toString() {
        AylaDevice d = getDevice();
        if ( d != null && d.dsn != null ) {
            return "Ayla DevKit " + getDevice().dsn;
        } else {
            return "Ayla DevKit";
        }
    }

    @Override
    public void onClick(View v) {
        // The green or blue LED has been tapped.
        boolean isGreenButton = (v.getId() == R.id.green_button);

        Log.i(LOG_TAG, "Button tapped: " + (isGreenButton ? "GREEN" : "BLUE"));
        if ( isGreenButton ) {
            setGreenLED(!isGreenLEDOn());
        } else {
            setBlueLED(!isBlueLEDOn());
        }
    }
}
