package com.aylanetworks.agilelink.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.internal.view.menu.MenuBuilder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.Toast;

import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaShare;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.aaml.AylaUser;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.fragments.adapters.NestedMenuAdapter;
import com.aylanetworks.agilelink.framework.SessionManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/*
 * SettingsFragment.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 1/28/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class SettingsFragment extends Fragment implements ExpandableListView.OnGroupClickListener, ExpandableListView.OnChildClickListener {
    private final static String LOG_TAG = "SettingsFragment";
    private ExpandableListView _listView;
    private Menu _menu;

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        _listView = (ExpandableListView)view.findViewById(R.id.listView);

        // Load the menu resource. We will be using the menu items in our listview.
        _menu = new MenuBuilder(getActivity());
        new MenuInflater(getActivity()).inflate(R.menu.menu_settings, _menu);

        _listView.setAdapter(new NestedMenuAdapter(getActivity(), R.layout.navigation_drawer_item, R.id.nav_textview, _menu));
        _listView.setOnGroupClickListener(this);
        _listView.setOnChildClickListener(this);

        return view;
    }

    @Override
    public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
        MenuItem item = _menu.getItem(groupPosition);
        if ( item.hasSubMenu() ) {
            if ( parent.isGroupExpanded(groupPosition)) {
                parent.collapseGroup(groupPosition);
            } else {
                parent.expandGroup(groupPosition);
            }
        } else {
            MainActivity.getInstance().settingsMenuItemClicked(item);
        }
        return true;
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
        MenuItem item = _menu.getItem(groupPosition).getSubMenu().getItem(childPosition);
        MainActivity.getInstance().settingsMenuItemClicked(item);
        return true;
    }
}
