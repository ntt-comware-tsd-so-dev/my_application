package com.aylanetworks.agilelink;

import java.io.Serializable;

public class DevicePropertyHolder implements Serializable {

    public String mFriendlyName;
    public String mPropertyName;
    public boolean mReadOnly;
    public boolean mState;

    public DevicePropertyHolder() {
    }

    public DevicePropertyHolder(String friendlyName, String propertyName,
                                boolean readOnly, boolean state) {
        mFriendlyName = friendlyName;
        mPropertyName = propertyName;
        mReadOnly = readOnly;
        mState = state;
    }
}
