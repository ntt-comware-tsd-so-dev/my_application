package com.aylanetworks.agilelink;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.view.CardFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

public class DeviceFragment extends CardFragment {

    public static final String ARG_DEVICE_HOLDER = "device_holder";
    public static final String ARG_PROPERTY_NAME = "property_name";
    public static final String ARG_OVERVIEW_CARD = "overview_card";

    private LocalBroadcastManager mLocalBroadcastManager;

    public DeviceFragment() {
        super();
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
    }

    @Override
    public View onCreateContentView(LayoutInflater inflater, ViewGroup container,
                                     Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        boolean overviewCard = arguments.getBoolean(ARG_OVERVIEW_CARD);
        final DeviceHolder deviceHolder = (DeviceHolder) arguments.getSerializable(ARG_DEVICE_HOLDER);

        View root;
        if (overviewCard) {
            root = inflater.inflate(R.layout.card_device_overview, null);
            TextView name = (TextView) root.findViewById(R.id.name);
            name.setText(deviceHolder.getName());
        } else {
            root = inflater.inflate(R.layout.card_device_property, null);

            final String propertyName = arguments.getString(ARG_PROPERTY_NAME);
            boolean propertyState = deviceHolder.getBooleanProperty(propertyName);

            TextView property = (TextView) root.findViewById(R.id.property);
            property.setText(propertyName);

            Switch toggle = (Switch) root.findViewById(R.id.toggle);
            toggle.setChecked(propertyState);
            toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    Intent i = new Intent(MainActivity.INTENT_PROPERTY_TOGGLED);
                    i.putExtra(MainActivity.EXTRA_DEVICE_DSN, deviceHolder.getDsn());
                    i.putExtra(MainActivity.EXTRA_PROPERTY_NAME, propertyName);
                    i.putExtra(MainActivity.EXTRA_PROPERTY_STATE, isChecked);

                    mLocalBroadcastManager.sendBroadcast(i);
                }
            });
        }

        return root;
    }
}
