package com.aylanetworks.agilelink.fragments.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.aylanetworks.agilelink.R;

import org.w3c.dom.Text;

/**
 * Created by Brian King on 3/18/15.
 */
public class ContactViewHolder extends RecyclerView.ViewHolder {
    public TextView _contactNameTextView;

    public ContactViewHolder(View v) {
        super(v);
        _contactNameTextView = (TextView)v.findViewById(R.id.contact_name);
    }
}
