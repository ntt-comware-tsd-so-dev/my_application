package com.aylanetworks.agilelink.fragments.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.aylanetworks.agilelink.framework.AMAPCore;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaShare;
import com.aylanetworks.agilelink.R;

import java.util.ArrayList;

/*
 * ShareListAdapter.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 3/26/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */
public class ShareListAdapter extends ArrayAdapter<AylaShare> {

    public ShareListAdapter(Context c, ArrayList<AylaShare> shares) {
        super(c, android.R.layout.simple_list_item_2, android.R.id.text1, shares);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = super.getView(position, convertView, parent);
        TextView tv1 = (TextView)v.findViewById(android.R.id.text1);
        TextView tv2 = (TextView)v.findViewById(android.R.id.text2);

        AylaShare share = getItem(position);

        Log.d("ShareListAdapter", share.toString());

        if ( share.getResourceId() != null ) {
            AylaDevice device = AMAPCore.sharedInstance().getDeviceManager()
                    .deviceWithDSN(share.getResourceId());
            if ( device != null ) {
                tv1.setText(device.toString());
                tv1.setTextColor(getContext().getResources().getColor(R.color.card_text));
            } else {
                tv1.setText(share.getResourceId());
                tv1.setTextColor(getContext().getResources().getColor(R.color.destructive_bg));
            }
        } else {
            tv1.setText("");
        }

        tv2.setTypeface(null, Typeface.ITALIC);
        if (TextUtils.equals(share.getUserEmail(),
                AMAPCore.sharedInstance().getCurrentUser().getEmail())) {
            tv1.setTextColor(tv1.getResources().getColor(R.color.card_shared_text));
            tv2.setText(share.getOwnerProfile().email);
        } else {
            tv1.setTextColor(tv1.getResources().getColor(R.color.card_text));
            tv2.setText(share.getUserProfile().email);
        }
        return v;
    }

}
