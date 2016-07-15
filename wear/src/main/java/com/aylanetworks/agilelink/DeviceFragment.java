package com.aylanetworks.agilelink;

import android.app.Fragment;
import android.os.Bundle;
import android.support.wearable.view.WearableListView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class DeviceFragment extends Fragment {

    public static final String ARG_ROW = "row";
    public static final String ARG_ROW_COUNT = "row_count";
    public static final String ARG_DEVICE_HOLDER = "device_holder";
    public static final String ARG_IS_OVERVIEW_CARD = "is_overview_card";

    private WearableListView mPropertyListView;

    public DeviceFragment() {
        super();
    }

    public void setPropertyListViewPosition(int position) {
        if (mPropertyListView != null) {
            mPropertyListView.scrollToPosition(position);
        }
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

            int row = arguments.getInt(ARG_ROW);
            if (row > 0) {
                propertiesList.add(new RowPropertyHolder(RowPropertyHolder.RowType.TOP));
            }

            for (int i = 0; i < deviceHolder.getPropertyCount(); i++) {
                String propertyName = deviceHolder.getPropertyNameOrdered(i);
                propertiesList.add(deviceHolder.getBooleanProperty(propertyName));
            }

            if (row < arguments.getInt(ARG_ROW_COUNT) - 1) {
                propertiesList.add(new RowPropertyHolder(RowPropertyHolder.RowType.BOTTOM));
            }

            mPropertyListView = (WearableListView) root.findViewById(R.id.property_list);
            mPropertyListView.setVisibility(View.VISIBLE);
            mPropertyListView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            mPropertyListView.setAdapter(new PropertyListAdapter(getActivity(), deviceHolder.getDsn(), propertiesList));
            mPropertyListView.setGreedyTouchMode(true);
            mPropertyListView.addOnScrollListener((WearableListView.OnScrollListener) getActivity());
            mPropertyListView.setClickListener((WearableListView.ClickListener) getActivity());

            if (row > 0) {
                if (propertiesList.size() > 2) {
                    setPropertyListViewPosition(2);
                } else if (propertiesList.size() > 1) {
                    setPropertyListViewPosition(1);
                }
            }
        }

        return root;
    }
}
