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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.aylanetworks.agilelink.framework.AMAPCore;
import com.aylanetworks.agilelink.framework.ViewModel;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.fragments.DeviceDetailFragment;
import com.aylanetworks.aylasdk.AylaDeviceNode;
import com.aylanetworks.aylasdk.error.AylaError;

import java.util.ArrayList;
import java.util.List;

public class GenericGateway extends ViewModel {
    public GenericGateway(AylaDevice aylaDevice) {
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
        return _device.getProductName();
    }

    @Override
    public int getItemViewType() {
        return AgileLinkViewModelProvider.ITEM_VIEW_TYPE_GENERIC_DEVICE;
    }

    @Override
    public void bindViewHolder(RecyclerView.ViewHolder holder) {
        GenericDeviceViewHolder h = (GenericDeviceViewHolder) holder;
        h._deviceNameTextView.setText(_device.getProductName());
        if (h._deviceStatusTextView != null) {
            h._deviceStatusTextView.setText(getDeviceState());
        }
        if ( !isIcon()) {
            h._spinner.setVisibility(getDevice().getProperties() == null ? View.VISIBLE : View.GONE);
        } else {
            h._spinner.setVisibility(View.GONE);
        }

        Resources res = AMAPCore.sharedInstance().getContext().getResources();
        int color = isOnline() ? res.getColor(R.color.card_text) : res.getColor(R.color
                .disabled_text);
        if (_device.getGrant() != null) {
            // Yes, this device is shared.
            color = res.getColor(R.color.card_shared_text);
        }
        h._deviceNameTextView.setTextColor(color);
        h._currentDeviceModel = this;
    }

    @Override
    protected ArrayList<String> getPropertyNames() {
        // Get the superclass' property names (probably none)
        ArrayList<String> propertyNames = super.getPropertyNames();

        // Add our own
        propertyNames.add("attr_set_cmd");
        propertyNames.add("attr_set_result");
        propertyNames.add("attr_read_data");
        propertyNames.add("join_enable");
        propertyNames.add("join_status");

        return propertyNames;
    }
    @Override
    public AylaDevice.RegistrationType registrationType() {
        return AylaDevice.RegistrationType.ButtonPush;
    }

    @Override
    public boolean isGateway() {
        return true;
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
    public Fragment getRemoteFragment() {
        return null;
    }

    public static class GatewayTypeAdapter extends ArrayAdapter<ViewModel> {

        public boolean useProductName;

        public GatewayTypeAdapter(Context c, ViewModel[] objects, boolean productName) {
            super(c, R.layout.spinner_device_selection, objects);
            useProductName = productName;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            View spinner = inflater.inflate(R.layout.spinner_device_selection, parent, false);

            ViewModel d = getItem(position);

            ImageView iv = (ImageView) spinner.findViewById(R.id.device_image);
            iv.setImageDrawable(d.getDeviceDrawable(AMAPCore.sharedInstance().getContext()));

            TextView name = (TextView) spinner.findViewById(R.id.device_name);
            name.setText(useProductName ? d.getName() : d.deviceTypeName());

            return spinner;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getDropDownView(position, convertView, parent);
        }
    }

    /**
     * Interface used when scanning for and registering a gateway's device nodes
     */
    public interface GatewayNodeRegistrationListener {

        /**
         * Notify that a registration candidate has been successfully registered as a device.
         *
         * @param device Device
         * @param moreComing When register
         * @param tag Optional user specified data.
         */
        void gatewayRegistrationCandidateAdded(AylaDevice device, boolean moreComing, Object tag);

        /**
         * Notify that registration candidates are available, the UI is then
         * given a chance to allow the user to select which devices to actually
         * register.  Then call back to gateway.registerCandidates
         *
         * @param list List of AylaDeviceNode objects
         * @param tag Optional user specified data.
         */
        void gatewayRegistrationCandidates(List<AylaDeviceNode> list, Object tag);

        /**
         * Notify that the the processs of scanning and registering a gateway's
         * device nodes has completed.
         *
         * @param error an error, if one occurred, or null
         * @param messageResourceId String resource id to display a toast to the user.
         * @param tag Optional user specified data.
         */
        void gatewayRegistrationComplete(AylaError error, int messageResourceId, Object tag);
    }

}

