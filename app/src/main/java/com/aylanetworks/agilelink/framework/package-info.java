/*
 * package-info.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 3/24/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

/**
 * Core components of the Agile Link application framework.
 * <p>
 * This package the set of core classes used by the Agile Link application framework. Implementers
 * should not need to modify these classes directly, but rather should customize behavior by
 * deriving classes from some of the classes defined in this package.
 * <p>
 * The {@link com.aylanetworks.agilelink.framework.Device} class provides basic functionality for
 * Device objects, and should be overridden for each type of device the application will support.
 *
 * <p>
 * Implementers will also need to create a class derived from {@link com.aylanetworks.agilelink.framework.DeviceCreator}
 * to create Device objects of the appropriate type from {@link com.aylanetworks.aaml.AylaDevice}
 * objects.
 *
 * <p>
 * The {@link com.aylanetworks.agilelink.framework.SessionManager} class is used to configure the
 * application parameters, user information, and anything else needed to be set up before connecting
 * to the Ayla network. The SessionManager creates
 */
package com.aylanetworks.agilelink.framework;
