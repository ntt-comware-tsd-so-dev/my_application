package com.aylanetworks.agilelink.device;
/*
 * AMAP_Android
 *
 * Copyright 2015 Ayla Networks, all rights reserved
 */

import android.content.Context;
import android.graphics.drawable.Drawable;

public interface DeviceUIProvider {
    public Drawable getDeviceDrawable(Context context);
    public int getGridViewSpan();
    public String getName();
    public String deviceTypeName();
}
