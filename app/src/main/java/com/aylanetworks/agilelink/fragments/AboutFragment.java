package com.aylanetworks.agilelink.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.aylanetworks.agilelink.framework.AMAPCore;
import com.aylanetworks.aylasdk.AylaNetworks;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.aylasdk.AylaSystemSettings;

import java.util.ArrayList;
import java.util.List;

/*
 * AboutFragment.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 1/21/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class AboutFragment extends Fragment {
    protected ListView _listView;
    protected TextView _headerTextView;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_about, container, false);

        _headerTextView = (TextView)v.findViewById(R.id.page_header);
        _headerTextView.setText(getString(R.string.about_app, getString(R.string.app_name)));

        _listView = (ListView)v.findViewById(R.id.list_view);

        populateList();

        return v;
    }

    protected void populateList() {
        List<AboutItem> items = new ArrayList<>();
        AMAPCore.SessionParameters params = AMAPCore.sharedInstance().getSessionParameters();
        AylaSystemSettings settings = AylaNetworks.sharedInstance().getSystemSettings();

        items.add(new AboutItem(getString(R.string.service_type), settings.serviceType.toString()));
        items.add(new AboutItem(getString(R.string.app_version), params.appVersion));
        items.add(new AboutItem(getString(R.string.library_version), AylaNetworks.getVersion()));
        items.add(new AboutItem(getString(R.string.core_version), AMAPCore.CORE_VERSION));

        boolean cellEnabled = AylaNetworks.sharedInstance().getConnectivity().isCellEnabled();
        boolean wifiEnabled = AylaNetworks.sharedInstance().getConnectivity().isWifiEnabled();

        String cellText = cellEnabled ? getString(android.R.string.yes) : getString(android.R
                .string.no);
        String wifiText = wifiEnabled ? getString(android.R.string.yes) : getString(android.R
                .string.no);

        items.add(new AboutItem(getString(R.string.wifi_enabled), wifiText));
        items.add(new AboutItem(getString(R.string.cellular_enabled), cellText));

        _listView.setAdapter(new AboutListAdapter(getActivity(), items.toArray(new AboutItem[items.size()])));
    }

    protected class AboutItem {
        public AboutItem(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String name;
        public String value;
    }

    protected class AboutListAdapter extends ArrayAdapter<AboutItem> {
        public AboutListAdapter(Context context, AboutItem[] objects) {
            super(context, R.layout.about_list_item, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if ( convertView == null ) {
                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.about_list_item, parent, false);
            }

            AboutItem item = getItem(position);
            TextView tv = (TextView)convertView.findViewById(R.id.name_text);
            tv.setText(item.name);

            tv = (TextView)convertView.findViewById(R.id.value_text);
            tv.setText(item.value);

            return convertView;
        }
    }
}
