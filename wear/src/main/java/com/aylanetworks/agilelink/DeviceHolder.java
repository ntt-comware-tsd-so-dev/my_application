package com.aylanetworks.agilelink;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class DeviceHolder implements Serializable {

    private String mName;
    private String mDsn;
    private String mStatus;
    private LinkedHashMap<String, DevicePropertyHolder> mPropertyMap = new LinkedHashMap<>();

    public DeviceHolder(String name, String dsn, String status) {
        mName = name;
        mDsn = dsn;
        mStatus = status;
    }

    public String getDsn() {
        return mDsn;
    }

    public String getName() {
        return mName;
    }

    public String getStatus() {
        return mStatus;
    }

    public String getPropertyNameOrdered(int i) {
        return new ArrayList<>(mPropertyMap.keySet()).get(i);
    }

    public int getPropertyCount() {
        return mPropertyMap.size();
    }

    public DevicePropertyHolder getBooleanProperty(String propertyName) {
        return mPropertyMap.get(propertyName);
    }

    public void addBooleanProperty(DevicePropertyHolder holder) {
        mPropertyMap.put(holder.mPropertyName, holder);
    }
}
