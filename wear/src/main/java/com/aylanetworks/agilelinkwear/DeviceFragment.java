package com.aylanetworks.agilelinkwear;

import android.os.Bundle;
import android.support.wearable.view.CardFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

public class DeviceFragment extends CardFragment {

    public static final String ARG_DEVICE_HOLDER = "device_holder";
    public static final String ARG_PROPERTY_NAME = "property_name";
    public static final String ARG_OVERVIEW_CARD = "overview_card";

    public DeviceFragment() {
        super();
    }

    @Override
    public View onCreateContentView(LayoutInflater inflater, ViewGroup container,
                                     Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        boolean overviewCard = arguments.getBoolean(ARG_OVERVIEW_CARD);
        DeviceHolder deviceHolder = (DeviceHolder) arguments.getSerializable(ARG_DEVICE_HOLDER);

        View root;
        if (overviewCard) {
            root = inflater.inflate(R.layout.card_device_overview, null);
            TextView name = (TextView) root.findViewById(R.id.name);
            name.setText(deviceHolder.getName());
        } else {
            root = inflater.inflate(R.layout.card_device_property, null);

            String propertyName = arguments.getString(ARG_PROPERTY_NAME);
            boolean propertyState = deviceHolder.getBooleanProperty(propertyName);

            Switch property = (Switch) root.findViewById(R.id.property);
            property.setText(propertyName);
            property.setChecked(propertyState);
        }

        return root;
    }
}
