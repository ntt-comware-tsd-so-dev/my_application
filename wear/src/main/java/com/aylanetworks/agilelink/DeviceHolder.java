package com.aylanetworks.agilelink;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class DeviceHolder implements Serializable {

    private String mName;
    private String mDsn;
    private LinkedHashMap<String, DevicePropertyHolder> mPropertyMap = new LinkedHashMap<>();

    public DeviceHolder(String name, String dsn) {
        mName = name;
        mDsn = dsn;
    }

    public String getDsn() {
        return mDsn;
    }

    public String getName() {
        return mName;
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

    public void setBooleanProperty(String propertyName, boolean state) {
        DevicePropertyHolder holder = mPropertyMap.get(propertyName);
        holder.mState = state;

        mPropertyMap.put(propertyName, holder);
    }

    public void addBooleanProperty(DevicePropertyHolder holder) {
        mPropertyMap.put(holder.mPropertyName, holder);
    }
}
