package com.aylanetworks.agilelink.framework;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaProperty;
import com.aylanetworks.aaml.AylaSystemUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Brian King on 12/22/14.
 */
public class Device extends AylaDevice implements Comparable<Device> {

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
        return this.dsn.compareTo(another.dsn);
    }

    /** Gets the latest device status from the server and calls listener when done */
    public void updateStatus(final DeviceStatusListener listener) {
        final Map<String, String> getPropertyArguments = getPropertyArgumentMap();
        getProperties(new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == AylaNetworks.AML_ERROR_OK) {

                    // Update our properties
                    properties = AylaSystemUtils.gson.fromJson((String) msg.obj,
                            AylaProperty[].class);
                    Log.v(LOG_TAG, "request: " + getPropertyArguments);
                    Log.v(LOG_TAG, "Properties for " + productName + " [" + Device.this.getClass().getSimpleName() + "]");

                    for (AylaProperty prop : properties) {
                        Log.v(LOG_TAG, "Prop: " + prop.name + ": " + prop.value);
                    }
                    if (listener != null) {
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

    @Override
    public String toString() {
        return getModel();
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
