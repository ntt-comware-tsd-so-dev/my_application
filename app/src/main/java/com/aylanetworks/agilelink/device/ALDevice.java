package com.aylanetworks.agilelink.device;

import com.aylanetworks.aaml.AylaDevice;

/**
 * Created by Brian King on 12/22/14.
 */
public class ALDevice extends AylaDevice implements Comparable<ALDevice> {
    @Override
    public int compareTo(ALDevice another) {
        // Base class just compares DSNs.
        return this.dsn.compareTo(another.dsn);
    }
}
