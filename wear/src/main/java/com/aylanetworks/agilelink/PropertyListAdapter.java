package com.aylanetworks.agilelink;

import android.content.Context;
import android.support.wearable.view.WearableListView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;

public class PropertyListAdapter extends WearableListView.Adapter {

    private String mDeviceDsn;
    private ArrayList<DevicePropertyHolder> mProperties;
    private OnPropertyToggleListener mListener;
    private LayoutInflater mInflater;

    public PropertyListAdapter(Context context, String dsn, ArrayList<DevicePropertyHolder> properties) {
        mListener = (OnPropertyToggleListener) context;
        mInflater = LayoutInflater.from(context);

        mDeviceDsn = dsn;
        mProperties = properties;
    }

    public static class ItemViewHolder extends WearableListView.ViewHolder {

        public TextView mPropertyName;
        public Switch mReadWriteProperty;
        public RadioButton mReadOnlyProperty;

        public ItemViewHolder(View itemView) {
            super(itemView);
            mPropertyName = (TextView) itemView.findViewById(R.id.property_name);
            mReadWriteProperty = (Switch) itemView.findViewById(R.id.rw_property);
            mReadOnlyProperty = (RadioButton) itemView.findViewById(R.id.ro_property);
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

        final DevicePropertyHolder propertyHolder = mProperties.get(position);
        propertyName.setText(propertyHolder.mFriendlyName);

        if (propertyHolder.mReadOnly) {
            readOnlyProperty.setVisibility(View.VISIBLE);
            readOnlyProperty.setChecked(propertyHolder.mState);
        } else {
            readWriteProperty.setVisibility(View.VISIBLE);
            readWriteProperty.setChecked(propertyHolder.mState);
            readWriteProperty.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onPropertyToggled(mDeviceDsn, propertyHolder.mPropertyName, ((Switch) v).isChecked());
                }
            });
        }

        holder.itemView.setTag(position);
    }

    @Override
    public int getItemCount() {
        return mProperties.size();
    }

    public interface OnPropertyToggleListener {
        void onPropertyToggled(String deviceDsn, String propertyName, boolean propertyState);
    }
}

