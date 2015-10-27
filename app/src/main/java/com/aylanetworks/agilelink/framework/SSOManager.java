package com.aylanetworks.agilelink.framework;

import android.os.Handler;

import java.util.Map;

/**
 * SSOManager.java
 * AgileLink Application Framework
 * Created by Raji Pillay on 10/12/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

/**
 * Implementers should create a class derived from {@link com.aylanetworks.agilelink.framework.SSOManager}
 * to customize single sign on features according to the identity provider's user service
 *
 */
public abstract class SSOManager {

    public abstract void login(final Handler handle, final String userName, final String password);

    public abstract void updateUserInfo(final Handler mHandle, Map<String, String> callParams);

    public abstract void deleteUser(Handler handler);



}
