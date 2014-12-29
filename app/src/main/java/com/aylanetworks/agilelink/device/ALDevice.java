package com.aylanetworks.agilelink.device;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaProperty;
import com.aylanetworks.aaml.AylaSystemUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Brian King on 12/22/14.
 */
public class ALDevice extends AylaDevice implements Comparable<ALDevice> {
    private static final String LOG_TAG = "ALDevice";

    public static interface DeviceStatusListener {
        public void statusUpdated(ALDevice device);
    }

    @Override
    public int compareTo(ALDevice another) {
        // Base class just compares DSNs.
        return this.dsn.compareTo(another.dsn);
    }

    /** Gets the latest device status from the server and calls listener when done */
    public void updateStatus(final DeviceStatusListener listener) {
        Map<String, String> getPropertyArguments = getPropertyArgumentMap();
        Log.e(LOG_TAG, getClass().getSimpleName() + " args: " + getPropertyArguments);
        getProperties(new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if ( msg.what == AylaNetworks.AML_ERROR_OK ) {

                    // Update our properties
                    properties = AylaSystemUtils.gson.fromJson((String)msg.obj,
                            AylaProperty[].class);
                    Log.d(LOG_TAG, "Properties for " + productName + ":\n");
                    for ( AylaProperty prop : properties ) {
                        Log.d(LOG_TAG, "Prop: " + prop.name + ": " + prop.value);
                    }
                    if ( listener != null ) {
                        listener.statusUpdated(ALDevice.this);
                    }
                }
            }
        }, getPropertyArguments);
    }

    /** Returns the arguments for the call to getProperties(). Derived classes should override this
     * to return the correct array of properties to be fetched in updateStatus().
     * @return The map of properties to fetch from the server
     */
    protected ArrayList<String> getPropertyNames() {
        return new ArrayList<>();
    }

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
