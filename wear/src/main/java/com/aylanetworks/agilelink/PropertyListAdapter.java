package com.aylanetworks.agilelink;

import android.content.Context;
import android.support.wearable.view.WearableListView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;

public class PropertyListAdapter extends WearableListView.Adapter {

    private String mDeviceDsn;
    private ArrayList<DevicePropertyHolder> mProperties;
    private PropertyToggleListener mListener;
    private LayoutInflater mInflater;

    public PropertyListAdapter(Context context, String dsn, ArrayList<DevicePropertyHolder> properties) {
        mListener = (PropertyToggleListener) context;
        mInflater = LayoutInflater.from(context);

        mDeviceDsn = dsn;
        mProperties = properties;
    }

    public static class ItemViewHolder extends WearableListView.ViewHolder {

        public TextView mPropertyName;
        public Switch mReadWriteProperty;
        public RadioButton mReadOnlyProperty;
        public ImageView mRow;

        public ItemViewHolder(View itemView) {
            super(itemView);
            mPropertyName = (TextView) itemView.findViewById(R.id.property_name);
            mReadWriteProperty = (Switch) itemView.findViewById(R.id.rw_property);
            mReadOnlyProperty = (RadioButton) itemView.findViewById(R.id.ro_property);
            mRow = (ImageView) itemView.findViewById(R.id.row);
        }
    }

    @Override
    public WearableListView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ItemViewHolder(mInflater.inflate(R.layout.list_property, null));
    }

    @Override
    public void onBindViewHolder(WearableListView.ViewHolder holder, int position) {
        ItemViewHolder itemHolder = (ItemViewHolder) holder;
        TextView propertyName = itemHolder.mPropertyName;
        Switch readWriteProperty = itemHolder.mReadWriteProperty;
        RadioButton readOnlyProperty = itemHolder.mReadOnlyProperty;
        ImageView row = itemHolder.mRow;

        final DevicePropertyHolder propertyHolder = mProperties.get(position);
        if (propertyHolder instanceof RowPropertyHolder) {
            // Navigation row, show navigation arrow
            row.setVisibility(View.VISIBLE);
            RowPropertyHolder.RowType type = ((RowPropertyHolder) propertyHolder).mRowType;
            if (type == RowPropertyHolder.RowType.TOP) {
                row.setBackgroundResource(R.mipmap.up);
            } else if (type == RowPropertyHolder.RowType.BOTTOM) {
                row.setBackgroundResource(R.mipmap.down);
            }
        } else {
            // Property row, show property name, and a status RadioButton for "from device"
            // properties or a Switch for "to device" properties
            propertyName.setText(propertyHolder.mFriendlyName);

            if (propertyHolder.mReadOnly) {
                readOnlyProperty.setVisibility(View.VISIBLE);
                readOnlyProperty.setChecked(propertyHolder.mState);
            } else {
                readWriteProperty.setVisibility(View.VISIBLE);
                readWriteProperty.setChecked(propertyHolder.mState);
                readWriteProperty.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        mListener.onPropertyToggled(mDeviceDsn, propertyHolder.mPropertyName, isChecked);
                    }
                });
            }
        }

        itemHolder.itemView.setTag(propertyHolder);
    }

    @Override
    public int getItemCount() {
        return mProperties.size();
    }

    public interface PropertyToggleListener {
        void onPropertyToggled(String deviceDsn, String propertyName, boolean propertyState);
    }
}

