package com.aylanetworks.agilelink;

import android.app.Fragment;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class DeviceFragment extends Fragment {

    public static final String ARG_DEVICE_HOLDER = "device_holder";
    public static final String ARG_IS_OVERVIEW_CARD = "is_overview_card";

    private PropertyListView mPropertyListView;
    private ImageView mRowHint;

    public DeviceFragment() {
        super();
    }

    public int getPropertyListViewPosition() {
        if (mPropertyListView != null) {
            return mPropertyListView.getCentralPosition();
        } else {
            return 0;
        }
    }

    public void setPropertyListViewPosition(int position) {
        if (mPropertyListView != null) {
            mPropertyListView.scrollToPosition(position);
        }
    }

    public void showRowHint(boolean nextRow) {
        if (mRowHint == null || mRowHint.getVisibility() == View.VISIBLE) {
            return;
        }

        mRowHint.setBackgroundResource(nextRow ? R.mipmap.down : R.mipmap.up);

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mRowHint.getLayoutParams();
        params.gravity = Gravity.CENTER_HORIZONTAL | (nextRow ? Gravity.BOTTOM : Gravity.TOP);
        mRowHint.setLayoutParams(params);

        mRowHint.setVisibility(View.VISIBLE);
    }

    public void hideRowHint() {
        if (mRowHint == null || mRowHint.getVisibility() == View.INVISIBLE) {
            return;
        }

        mRowHint.setVisibility(View.INVISIBLE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                     Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        boolean overviewCard = arguments.getBoolean(ARG_IS_OVERVIEW_CARD);
        final DeviceHolder deviceHolder = (DeviceHolder) arguments.getSerializable(ARG_DEVICE_HOLDER);

        View root = inflater.inflate(R.layout.card_device, null);
        if (overviewCard) {
            TextView deviceName = (TextView) root.findViewById(R.id.device_name);
            deviceName.setText(deviceHolder.getName());

            TextView deviceStatus = (TextView) root.findViewById(R.id.device_status);
            deviceStatus.setText(deviceHolder.getStatus());

            LinearLayout deviceContainer = (LinearLayout) root.findViewById(R.id.overview_container);
            deviceContainer.setVisibility(View.VISIBLE);
        } else {
            ArrayList<DevicePropertyHolder> propertiesList = new ArrayList<>();
            for (int i = 0; i < deviceHolder.getPropertyCount(); i++) {
                String propertyName = deviceHolder.getPropertyNameOrdered(i);
                propertiesList.add(deviceHolder.getBooleanProperty(propertyName));
            }

            mRowHint = (ImageView) root.findViewById(R.id.row_hint);
            mRowHint.setVisibility(View.VISIBLE);

            mPropertyListView = (PropertyListView) root.findViewById(R.id.property_list);
            mPropertyListView.setScrollStatusListener((PropertyListView.ScrollStatusListener) getActivity());
            mPropertyListView.setVisibility(View.VISIBLE);
            mPropertyListView.setAdapter(new PropertyListAdapter(getActivity(), deviceHolder.getDsn(), propertiesList));
            mPropertyListView.setGreedyTouchMode(true);
        }

        return root;
    }
}
