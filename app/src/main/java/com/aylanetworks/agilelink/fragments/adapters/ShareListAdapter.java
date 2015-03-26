package com.aylanetworks.agilelink.fragments.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.aylanetworks.aaml.AylaShare;
import com.aylanetworks.aaml.AylaUser;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.SessionManager;

/*
 * ShareListAdapter.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 3/26/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */
public class ShareListAdapter extends ArrayAdapter<AylaShare> {
    public ShareListAdapter(Context c, AylaShare[] objects) {
        super(c, android.R.layout.simple_list_item_2, android.R.id.text1, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = super.getView(position, convertView, parent);
        TextView tv1 = (TextView)v.findViewById(android.R.id.text1);
        TextView tv2 = (TextView)v.findViewById(android.R.id.text2);

        AylaShare share = getItem(position);

        Log.d("ShareListAdapter", share.toString());

        if ( share.resourceId != null ) {
            Device device = SessionManager.deviceManager().deviceByDSN(share.resourceId);
            if ( device != null ) {
                tv1.setText(device.toString());
                tv1.setTextColor(getContext().getResources().getColor(R.color.card_text));
            } else {
                tv1.setText(share.resourceId);
                tv1.setTextColor(getContext().getResources().getColor(R.color.destructive_bg));
            }
        } else {
            tv1.setText("");
        }

        tv2.setTypeface(null, Typeface.ITALIC);
        if ( share.userProfile.email.equals(AylaUser.getCurrent().email)) {
            tv1.setTextColor(tv1.getResources().getColor(R.color.card_shared_text));
            tv2.setText(share.ownerProfile.email);
        } else {
            tv1.setTextColor(tv1.getResources().getColor(R.color.card_text));
            tv2.setText(share.userProfile.email);
        }
        return v;
    }

}
