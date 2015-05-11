package com.aylanetworks.agilelink.framework;

/*
 * UIConfig.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 4/9/2015.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

/**
 * The UIConfig class is used to configure the look and feel of the application. Currently
 * the ListStyle can be set to a List or Grid view for device lists, and the NavStyle
 * can be set to Pager for a 3-pane paged view, or Drawer for a navigation drawer style app.
 */
public class UIConfig {
    /** Enumeration of styles the list views can be shown in (all devices / device groups) */
    public enum ListStyle { List, Grid }

    /** Enumeration of navigation styles the app can be configured with */
    public enum NavStyle { Pager, Drawer }

    /** Style for all devices / device group list views */
    public ListStyle _listStyle = ListStyle.List;

    /** Overall navigation style for the app */
    public NavStyle _navStyle = NavStyle.Pager;
}
