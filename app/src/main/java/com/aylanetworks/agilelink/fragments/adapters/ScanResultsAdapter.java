package com.aylanetworks.agilelink.fragments.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.aylanetworks.aylasdk.AylaHostScanResults;

/*
 * ScanResultsAdapter.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 1/28/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class ScanResultsAdapter extends ArrayAdapter<AylaHostScanResults> {
    public ScanResultsAdapter(Context c, AylaHostScanResults[] objects) {
        super(c, android.R.layout.simple_list_item_1, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = super.getView(position, convertView, parent);
        TextView tv = (TextView)v.findViewById(android.R.id.text1);

        AylaHostScanResults results = getItem(position);
        tv.setText(results.ssid);
        tv.setTextAppearance(tv.getContext(), android.R.style.TextAppearance_Medium);
        tv.setTypeface(null, Typeface.BOLD);
        return v;
    }
}
