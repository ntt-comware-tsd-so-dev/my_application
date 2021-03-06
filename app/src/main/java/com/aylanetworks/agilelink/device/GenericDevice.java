package com.aylanetworks.agilelink.device;
/*
 * AMAP_Android
 *
 * Copyright 2015 Ayla Networks, all rights reserved
 */

import android.content.Context;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.aylanetworks.agilelink.framework.AMAPCore;
import com.aylanetworks.agilelink.framework.ViewModel;
import com.aylanetworks.agilelink.framework.ViewModelProvider;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.fragments.DeviceDetailFragment;
import com.aylanetworks.agilelink.fragments.NotificationListFragment;
import com.aylanetworks.agilelink.fragments.ScheduleContainerFragment;

/**
 * <ul>
 * Derived classes should override the following methods:
 * <li>{@link #getItemViewType()}</li>
 * <li>{@link #bindViewHolder(android.support.v7.widget.RecyclerView.ViewHolder)}</li>
 * <li>{@link #getSchedulablePropertyNames()}</li>
 * <li>{@link #friendlyNameForPropertyName(String)}</li>
 * </ul>
 */
public class GenericDevice extends ViewModel {
    /**
     * Constructor using the AylaDevice parameter
     *
     * @param aylaDevice AylaDevice object this device represents
     */
    public GenericDevice(AylaDevice aylaDevice) {
        super(aylaDevice);
    }

    /**
     * Updates the views in the ViewHolder with information from the Device object.
     * <p/>
     * Derived classes should override this method to set up a ViewHolder for display in
     * RecyclerViews.
     *
     * @param holder The view holder for this object
     */
    public void bindViewHolder(RecyclerView.ViewHolder holder) {
        final GenericDeviceViewHolder h = (GenericDeviceViewHolder) holder;
        h._deviceNameTextView.setText(_device.getProductName());
        if (h._deviceStatusImageView != null) {
            h._deviceStatusImageView.setImageDrawable(getStatusDrawable());
        }

        Resources res = AMAPCore.sharedInstance().getContext().getResources();
        int color;

        if (isIcon()) {
            h._spinner.setVisibility(View.GONE);
            color = res.getColor(R.color.card_text);
        } else {
            h._spinner.setVisibility(getDevice().getProperties() == null ? View.VISIBLE : View.GONE);

            if (h._expandedLayout != null) {
                h._expandedLayout.setVisibility(h.getPosition() == GenericDeviceViewHolder._expandedIndex ? View.VISIBLE : View.GONE);
                // Set up handlers for the buttons in the expanded view
                h._notificationsButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        MainActivity.getInstance().pushFragment(NotificationListFragment
                                .newInstance(GenericDevice.this));
                    }
                });
                h._notificationsButton.setVisibility(getNotifiablePropertyNames().length > 0 ? View.VISIBLE : View.GONE);

                h._scheduleButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        MainActivity.getInstance().pushFragment(ScheduleContainerFragment
                                .newInstance(GenericDevice.this));
                    }
                });
                h._scheduleButton.setVisibility(getSchedulablePropertyNames().length > 0 ? View.VISIBLE : View.GONE);

                h._detailsButton.setColorFilter(MainActivity.getInstance().getResources().getColor(R.color.card_text), PorterDuff.Mode.SRC_ATOP);
                h._detailsButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        MainActivity.getInstance().pushFragment(DeviceDetailFragment
                                .newInstance(GenericDevice.this));
                    }
                });
            }

            // Is this a shared device?
            color = isOnline() ? res.getColor(R.color.card_text) : res.getColor(R.color.disabled_text);
            if (getDevice().getGrant() != null) {
                // If we have a grant, this device is shared with us.
                color = res.getColor(R.color.card_shared_text);
            }
        }
        h._deviceNameTextView.setTextColor(color);
        h._currentDeviceModel = this;
    }
    /**
     * Returns an integer representing the item view type for this device. This method is called
     * when displaying a CardView representing this device. The item view type should be different
     * for each type of CardView displayed for a device.
     * <p>
     * The value returned from this method will be passed to the
     * {@link AMAPViewModelProvider#viewHolderForViewType
     * (android.view.ViewGroup, int)}
     * method of the {@link ViewModelProvider} object, which uses
     * it to determine the appropriate ViewHolder object to create for the Device.
     * <p>
     * Multiple device types may use the same item view type if the view displayed for these devices
     * are the same. Most devices will have their own unique views displayed.
     * <p>
     * View types should be unique, and are generally defined as static members of the
     * {@link AMAPViewModelProvider} class. This keeps them all in the same place and makes it
     * easy to
     * ensure that each identifier is unique.
     *
     * @return An integer representing the type of view for this item.
     */
    public int getItemViewType() {
        return AMAPViewModelProvider.ITEM_VIEW_TYPE_GENERIC_DEVICE;
    }

    /**
     * Returns a fragment used to display details about the device. This fragment is pushed onto
     * the back stack when the user selects an item from the device list.
     *
     * @return a fragment showing device details
     */
    public Fragment getDetailsFragment() {
        return DeviceDetailFragment.newInstance(this);
    }

    /**
     * Returns a fragment used to set up a schedule for this device. This fragment is pushed onto
     * the back stack when the user taps the Schedules button from the device details page
     *
     * @return a Fragment used to configure schedules for this device
     */
    public Fragment getScheduleFragment() {
        return ScheduleContainerFragment.newInstance(this);
    }

    /**
     * Returns a fragment used to set up notifications for this device. The default implementation
     * returns a generic PropertyNotificationFragment.
     *
     * @return a Fragment used to configure property notifications for this device
     */
    public Fragment getNotificationsFragment() {
        return NotificationListFragment.newInstance(this);
    }

    public Drawable getStatusDrawable() {
        Context context = AMAPCore.sharedInstance().getContext();
        Drawable image = null;
        if ( context != null ) {
            image = ContextCompat.getDrawable(context, R.drawable.ic_cloud_off_black_24dp);
            int imageColor = ContextCompat.getColor(context, R.color.colorIconError);
            if (_device.getConnectionStatus().equals(AylaDevice.ConnectionStatus.Online)) {
                imageColor = ContextCompat.getColor(context, R.color.green_600);
                if (_device.isLanModeActive()) {
                    image = ContextCompat.getDrawable(context,
                            R.drawable.ic_signal_wifi_4_bar_black_24dp);
                } else {
                    image = ContextCompat.getDrawable(context,
                            R.drawable.ic_cloud_circle_black_24dp);
                }
            }
            image.setColorFilter(imageColor, PorterDuff.Mode.SRC_ATOP);
        }
        return image;
    }

    /**
     * Returns the number of elements the device's view should span when displayed in a grid view.
     * The default value is 1. Override this in your device if the device's grid view requires
     * more room.
     *
     * @return The number of columns the device's view should span when displayed in a grid
     */
    @Override
    public int getGridViewSpan() {
        return 1;
    }

    @Override
    public String getName() {
        return getDevice().getProductName();
    }

    /**
     * Returns a Drawable representing the device (thumbnail image)
     *
     * @param c Context to access resources
     * @return A Drawable object representing the device
     */
    @Override
    public Drawable getDeviceDrawable(Context c) {
        return c.getResources().getDrawable(R.drawable.generic_device);
    }

}
