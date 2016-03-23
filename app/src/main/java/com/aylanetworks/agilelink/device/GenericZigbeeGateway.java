package com.aylanetworks.agilelink.device;
/*
 * AMAP_Android
 *
 * Copyright 2015 Ayla Networks, all rights reserved
 */

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.fragments.DeviceDetailFragment;
import com.aylanetworks.agilelink.framework.GenericDeviceViewHolder;
import com.aylanetworks.agilelink.framework.SessionManager;
import com.aylanetworks.agilelink.framework.ZigbeeGateway;

public class GenericZigbeeGateway extends ZigbeeGateway implements DeviceUIProvider {
    public GenericZigbeeGateway(AylaDevice aylaDevice) {
        super(aylaDevice);
    }

    public Drawable getDeviceDrawable(Context context) {
        return ContextCompat.getDrawable(context, R.drawable.ic_generic_gateway_red);
    }

    @Override
    public int getGridViewSpan() {
        return 1;
    }

    @Override
    public String getName() {
        return getProductName();
    }

    @Override
    public int getItemViewType() {
        return AgileLinkDeviceCreator.ITEM_VIEW_TYPE_GENERIC_DEVICE;
    }

    @Override
    public void bindViewHolder(RecyclerView.ViewHolder holder) {
        GenericDeviceViewHolder h = (GenericDeviceViewHolder) holder;
        h._deviceNameTextView.setText(getProductName());
        if (h._deviceStatusTextView != null) {
            h._deviceStatusTextView.setText(getDeviceState());
        }
        if ( !isIcon()) {
            h._spinner.setVisibility(getDevice().properties == null ? View.VISIBLE : View.GONE);
        } else {
            h._spinner.setVisibility(View.GONE);
        }

        Resources res = SessionManager.getContext().getResources();
        int color = isOnline() ? res.getColor(R.color.card_text) : res.getColor(R.color
                .disabled_text);
        if (!getDevice().amOwner()) {
            // Yes, this device is shared.
            color = res.getColor(R.color.card_shared_text);
        }
        h._deviceNameTextView.setTextColor(color);
        h._currentDevice = this;
    }

    @Override
    public Fragment getDetailsFragment() {
        return DeviceDetailFragment.newInstance(this);
    }

    @Override
    public Fragment getScheduleFragment() {
        return null;
    }

    @Override
    public Fragment getTriggerFragment() {
        return null;
    }

    @Override
    public Fragment getRemoteFragment() {
        return null;
    }
}
