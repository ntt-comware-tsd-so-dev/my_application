package com.aylanetworks.agilelink.framework;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaProperty;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.fragments.DeviceDetailFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Brian King on 12/22/14.
 */
public class Device implements Comparable<Device> {

    public interface DeviceStatusListener {
        void statusUpdated(Device device);
    }

    private static final String LOG_TAG = "Device";

    /**
     * Default comparator. Sorts alphabetically by DSN.
     * @param another Device to compare to
     * @return the standard comparator result
     */
    @Override
    public int compareTo(Device another) {
        // Base class just compares DSNs.
        return this.getDevice().dsn.compareTo(another.getDevice().dsn);
    }

    /** The AylaDevice object wrapped by this class */
    private AylaDevice _device;

    public AylaDevice getDevice() {
        return _device;
    }

    /** Constructor using the AylaDevice parameter */
    public Device(AylaDevice aylaDevice) {
        _device = aylaDevice;
    }

    /** Private default constructor. */
    private Device() {
        // Private constructor. Do not use.
    }

    /** Gets the latest device status from the server and calls listener when done */
    public void updateStatus(final DeviceStatusListener listener) {
        final Map<String, String> getPropertyArguments = getPropertyArgumentMap();
        getDevice().getProperties(new Handler() {
            @Override
            public void handleMessage(Message msg) {
                AylaDevice d = getDevice();

                if (msg.what == AylaNetworks.AML_ERROR_OK) {
                    // Update our properties
                    d.properties = AylaSystemUtils.gson.fromJson((String) msg.obj,
                            AylaProperty[].class);
                    Log.v(LOG_TAG, "request: " + getPropertyArguments);
                    Log.v(LOG_TAG, "Properties for " + d.productName + " [" + Device.this.getClass().getSimpleName() + "]");

                    for (AylaProperty prop : d.properties) {
                        Log.v(LOG_TAG, "Prop: " + prop.name + ": " + prop.value);
                    }
                    if (listener != null) {
                        listener.statusUpdated(Device.this);
                    }
                } else {
                    Log.e(LOG_TAG, "Failed to get properties for " + d.getProductName() + ": error " + msg.what);
                    if ( listener != null ) {
                        listener.statusUpdated(Device.this);
                    }
                }
            }
        }, getPropertyArguments);
    }

    /**
     * Returns the AylaProperty of the given name, or null if no property was found.
     * @param propertyName Name of the property to return
     * @return AylaProperty of the given name, or null if not found
     */
    public AylaProperty getProperty(String propertyName) {
        AylaProperty[] properties = getDevice().properties;
        if ( properties == null )
            return null;

        for ( AylaProperty prop : properties ) {
            if ( prop.name.equals(propertyName))
                return prop;
        }
        return null;
    }

    public boolean isGateway() {
        return false;
    }

    /** UI Methods */

    /**
     * Returns a view representing this device to be used in a ListView container
     * @param c Context
     * @param convertView Old view to reuse, if possible
     * @param parent Parent for the returned view
     * @return A view representing the device as shown in a ListView
     */
    public View getListItemView(Context c, View convertView, ViewGroup parent) {
        // If we don't have a view to reuse, or if it's not of the type we expect, create a
        // new one. Note that we don't have a particular view we require here, so no check is done
        // for the appropriate class type.
        if ( convertView == null ) {
            convertView = LayoutInflater.from(c).inflate(R.layout.default_list_item, parent, false);
        }

        TextView deviceNameTextView = (TextView)convertView.findViewById(R.id.device_name);
        TextView deviceStateTextView = (TextView)convertView.findViewById(R.id.device_state);

        deviceNameTextView.setText(toString());
        deviceStateTextView.setText(getDeviceState());

        return convertView;
    }

    public Drawable getDeviceDrawable(Context c) {
        return c.getResources().getDrawable(R.drawable.generic_device);
    }

    public Fragment getDetailsFragment(Context c) {
        return new DeviceDetailFragment();
    }

    @Override
    public String toString() {
        return getDevice().getModel();
    }

    /**
     * Returns a string representing the state of the device (on, off, open, closed, etc.)
     * The default implementation returns nothing.
     * @return A string representing the state of the device
     */
    public String getDeviceState() {
        return "";
    }

    /** Returns the arguments for the call to getProperties(). Derived classes should override this
     * to return the correct array of properties to be fetched in updateStatus().
     * @return The map of properties to fetch from the server
     */
    protected ArrayList<String> getPropertyNames() {
        return new ArrayList<>();
    }

    /** Turns the array of property names returned from getPropertyNames() into a space-
     * separated string. This is the format required by the library for a list of properties to
     * fetch.
     * @return a map of the property arguments that can be directly submitted to a getProperties()
     * request.
     */
    protected Map<String, String> getPropertyArgumentMap() {
        ArrayList<String> myProperties = getPropertyNames();
        if ( myProperties.size() == 0 ) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for ( String name : myProperties ) {
            if ( !first ) {
                sb.append(" ");
            } else {
                first = false;
            }
            sb.append(name);
        }

        Map<String, String> paramMap = new HashMap<String, String>();
        paramMap.put("names", sb.toString());
        return paramMap;
    }
}
