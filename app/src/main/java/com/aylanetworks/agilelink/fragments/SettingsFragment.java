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
import android.widget.ListView;
import android.widget.Toast;

import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaShare;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.aaml.AylaUser;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
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

public class SettingsFragment extends Fragment implements AdapterView.OnItemClickListener {
    private final static String LOG_TAG = "SettingsFragment";
    private ListView _listView;
    private List<MenuItem> _menuItems;

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        _listView = (ListView)view.findViewById(R.id.listView);

        // Load the menu resource. We will be using the menu items in our listview.
        Menu menu = new MenuBuilder(getActivity());
        new MenuInflater(getActivity()).inflate(R.menu.menu_settings, menu);

        _menuItems = new ArrayList<MenuItem>();
        for ( int i = 0; i < menu.size(); i++ ) {
            MenuItem item = menu.getItem(i);
            Log.d(LOG_TAG, "Menu item " + i + ": " + item);
            _menuItems.add(item);
        }

        ArrayAdapter<MenuItem>adapter = new ArrayAdapter<MenuItem>(getActivity(), android.R.layout.simple_list_item_1, _menuItems);
        _listView.setAdapter(adapter);
        _listView.setOnItemClickListener(this);

        return view;
    }



    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d(LOG_TAG, "onItemClick: " + position);
        MenuItem item = _menuItems.get(position);
        MainActivity.getInstance().settingsMenuItemClicked(item);

    }
}
