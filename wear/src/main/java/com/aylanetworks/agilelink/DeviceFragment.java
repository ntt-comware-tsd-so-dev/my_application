package com.aylanetworks.agilelink;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;

public class DeviceFragment extends Fragment {

    public static final String ARG_DEVICE_HOLDER = "device_holder";
    public static final String ARG_DEVICE_DRAWABLE = "device_drawable";
    public static final String ARG_DEVICE_PROPERTY = "device_property";
    public static final String ARG_IS_OVERVIEW_CARD = "is_overview_card";

    private OnPropertyToggleListener mListener;

    public DeviceFragment() {
        super();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mListener = (OnPropertyToggleListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnHeadlineSelectedListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                     Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        boolean overviewCard = arguments.getBoolean(ARG_IS_OVERVIEW_CARD);
        final DeviceHolder deviceHolder = (DeviceHolder) arguments.getSerializable(ARG_DEVICE_HOLDER);

        View root = inflater.inflate(R.layout.card_device, null);
        TextView name = (TextView) root.findViewById(R.id.name);

        if (overviewCard) {
            name.setText(deviceHolder.getName());
            /**
             BitmapDrawable deviceDrawable = new BitmapDrawable(getResources(), (Bitmap) arguments.getParcelable(ARG_DEVICE_DRAWABLE));
             ImageView icon = (ImageView) root.findViewById(R.id.device_drawable);
             icon.setBackground(deviceDrawable);
             */ // TODO: MAKE IT PRETTY
        } else {
            final DevicePropertyHolder propertyHolder = deviceHolder.getBooleanProperty(arguments.getString(ARG_DEVICE_PROPERTY));

            name.setText(propertyHolder.mFriendlyName);
            if (propertyHolder.mReadOnly) {
                RadioButton readOnlyProperty = (RadioButton) root.findViewById(R.id.ro_property);
                readOnlyProperty.setVisibility(View.VISIBLE);
                readOnlyProperty.setChecked(propertyHolder.mState);
            } else {
                Switch readWriteProperty = (Switch) root.findViewById(R.id.rw_property);
                readWriteProperty.setVisibility(View.VISIBLE);
                readWriteProperty.setChecked(propertyHolder.mState);
                readWriteProperty.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        mListener.onPropertyToggled(deviceHolder.getDsn(),
                                propertyHolder.mPropertyName,
                                ((Switch) v).isChecked());
                    }
                });
            }
        }

        return root;
    }

    public interface OnPropertyToggleListener {
        void onPropertyToggled(String deviceDsn, String propertyName, boolean propertyState);
    }
}
