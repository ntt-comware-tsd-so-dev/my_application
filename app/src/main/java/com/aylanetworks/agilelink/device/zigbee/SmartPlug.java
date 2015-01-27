package com.aylanetworks.agilelink.device.zigbee;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.aylanetworks.aaml.AylaDatapoint;
import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaProperty;
import com.aylanetworks.agilelink.AgileLinkApplication;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.Device;

import java.util.ArrayList;

/**
 * Created by Brian King on 12/19/14.
 */
public class SmartPlug extends Device implements CompoundButton.OnCheckedChangeListener {
    private final static String LOG_TAG = "SmartPlug";
    private final static String PROPERTY_SWITCH_ON = "1_in_0x0006_0x0000";
    private final static int SWITCH_ON = 1;

    public SmartPlug(AylaDevice aylaDevice) {
        super(aylaDevice);
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

    public boolean isOn() {
        AylaProperty onProp = getProperty(PROPERTY_SWITCH_ON);
        if ( onProp != null && onProp.value != null ) {
            return (Integer.parseInt(onProp.value) == SWITCH_ON);
        }
        // Unknown
        Log.i(LOG_TAG, "No property value for SWITCH_ON!");
        return false;
    }

    @Override
    protected ArrayList<String> getPropertyNames() {
        // Get the superclass' property names (probably none)
        ArrayList<String> propertyNames = super.getPropertyNames();

        // Add our own
        propertyNames.add(PROPERTY_SWITCH_ON);

        return propertyNames;
    }

    @Override
    public String getDeviceState() {
        Context c = AgileLinkApplication.getAppContext();
        if ( getProperty(PROPERTY_SWITCH_ON) == null ) {
            return c.getString(R.string.device_state_unknown);
        }

        String on = isOn() ? c.getString(R.string.on) : c.getString(R.string.off);
        return on;
    }

//    @Override
//    public View getListItemView(Context c, View convertView, ViewGroup parent) {
//        if ( convertView == null || convertView.findViewById(R.id.toggle_switch) == null ) {
//            convertView = LayoutInflater.from(c).inflate(R.layout.on_off_list_item, parent, false);
//        }
//
//        Switch s = (Switch)convertView.findViewById(R.id.toggle_switch);
//
//        s.setOnCheckedChangeListener(null);
//        s.setChecked(isOn());
//        s.setOnCheckedChangeListener(this);
//        s.setText(R.string.smart_plug);
//
//        return convertView;
//    }


    private Handler _createDatapointHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.i(LOG_TAG, "SmartPlug: createDatapointHandler called: " + msg);
        }
    };

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Log.i(LOG_TAG, toString() + "onCheckedChanged");
        AylaProperty onProperty = getProperty(PROPERTY_SWITCH_ON);
        if ( onProperty == null ) {
            Log.e(LOG_TAG, "Couldn't find property " + PROPERTY_SWITCH_ON + " to set!");
            return;
        }

        if ( isChecked && isOn() ) {
            Log.i(LOG_TAG, "Device is already in the correct state. Not changing.");
        }

        buttonView.setEnabled(false);
        AylaDatapoint dp = new AylaDatapoint();
        dp.nValue(isChecked ? 1 : 0);
        try {
            onProperty.createDatapoint(_createDatapointHandler, dp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
