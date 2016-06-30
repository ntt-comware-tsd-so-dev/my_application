package com.aylanetworks.agilelink;

import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;

public class DeviceFragment extends Fragment {

    public static final String ARG_DEVICE_HOLDER = "device_holder";
    public static final String ARG_DEVICE_DRAWABLE = "device_drawable";
    public static final String ARG_DEVICE_PROPERTY = "device_property";
    public static final String ARG_IS_OVERVIEW_CARD = "is_overview_card";

    private LocalBroadcastManager mLocalBroadcastManager;

    public DeviceFragment() {
        super();
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                     Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        boolean overviewCard = arguments.getBoolean(ARG_IS_OVERVIEW_CARD);
        final DeviceHolder deviceHolder = (DeviceHolder) arguments.getSerializable(ARG_DEVICE_HOLDER);

        View root;
        if (overviewCard) {
            root = inflater.inflate(R.layout.card_device_overview, null);
            TextView name = (TextView) root.findViewById(R.id.name);
            name.setText(deviceHolder.getName());

            BitmapDrawable deviceDrawable = new BitmapDrawable(getResources(), (Bitmap) arguments.getParcelable(ARG_DEVICE_DRAWABLE));
            ImageView icon = (ImageView) root.findViewById(R.id.device_drawable);
            icon.setBackground(deviceDrawable);
            icon.setVisibility(View.GONE); // TODO: MAKE IT PRETTY
        } else {
            final DevicePropertyHolder propertyHolder = (DevicePropertyHolder) arguments.getSerializable(ARG_DEVICE_PROPERTY);
            if (propertyHolder.mReadOnly) {
                root = inflater.inflate(R.layout.card_device_property_ro, null);

                TextView property = (TextView) root.findViewById(R.id.property);
                property.setText(propertyHolder.mFriendlyName);

                RadioButton status = (RadioButton) root.findViewById(R.id.status);
                status.setChecked(propertyHolder.mState);
            } else {
                root = inflater.inflate(R.layout.card_device_property_rw, null);

                TextView property = (TextView) root.findViewById(R.id.property);
                property.setText(propertyHolder.mFriendlyName);

                Switch status = (Switch) root.findViewById(R.id.status);
                status.setChecked(propertyHolder.mState);
                status.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        Intent i = new Intent(MainActivity.INTENT_PROPERTY_TOGGLED);
                        i.putExtra(MainActivity.EXTRA_DEVICE_DSN, deviceHolder.getDsn());
                        i.putExtra(MainActivity.EXTRA_PROPERTY_NAME, propertyHolder.mPropertyName);
                        i.putExtra(MainActivity.EXTRA_PROPERTY_STATE, isChecked);

                        mLocalBroadcastManager.sendBroadcast(i);
                    }
                });
            }
        }

        return root;
    }
}
