package com.aylanetworks.agilelink.device;
/* 
 * RemoteSwitchDevice
 * AgileLink Application Framework
 *
 * Created by David N. Junod on 6/3/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

import android.os.Message;
import com.aylanetworks.agilelink.framework.Device;
import java.util.List;

public interface RemoteSwitchDevice {

    public interface RemoteSwitchCompletionHandler {

        public void handle(RemoteSwitchDevice remote, Message msg, Object tag);

    }

    public boolean isPairableDevice(Device device);

    public boolean isDevicePaired(Device device);

    public List<Device> getPairedDevices();

    public void pairDevice(Device device, Object tag, RemoteSwitchCompletionHandler completion);

    public void pairDevices(List<Device> list, Object tag, RemoteSwitchCompletionHandler completion);

    public void unpairDevice(Device device, Object tag, RemoteSwitchCompletionHandler completion);

    public void unpairDevices(List<Device> list, Object tag, RemoteSwitchCompletionHandler completion);
}

