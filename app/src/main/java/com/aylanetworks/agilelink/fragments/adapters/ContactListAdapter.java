package com.aylanetworks.agilelink.fragments.adapters;

import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.aylanetworks.aaml.AylaContact;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.SessionManager;

import java.util.List;

/**
 * ContactListAdapter.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 3/18/2015.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */
public class ContactListAdapter extends RecyclerView.Adapter {
    private final static String LOG_TAG = "ContactListAdapter";

    public interface ContactCardListener {
        public enum IconType { ICON_EMAIL, ICON_SMS };
        void emailTapped(AylaContact contact);
        void smsTapped(AylaContact contact);
        void contactTapped(AylaContact contact);
        void contactLongTapped(AylaContact contact);
        int colorForIcon(AylaContact contact, IconType iconType);
    }

    protected List<AylaContact> _aylaContacts;
    private ContactCardListener _listener;

    public ContactListAdapter(boolean includeOwner, ContactCardListener listener) {
        _listener = listener;
        _aylaContacts = SessionManager.getInstance().getContactManager().getContacts(includeOwner);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_contact, parent, false);
        return new ContactViewHolder(v, _listener);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ContactViewHolder h = (ContactViewHolder) holder;
        AylaContact contact = _aylaContacts.get(position);

        if (TextUtils.isEmpty(contact.displayName) || TextUtils.isEmpty(contact.displayName.trim())) {
            if (TextUtils.isEmpty(contact.firstname) && TextUtils.isEmpty(contact.lastname)) {
                h._contactNameTextView.setText(contact.email);
            } else {
                h._contactNameTextView.setText(contact.firstname + " " + contact.lastname);
            }
        } else {
            h._contactNameTextView.setText(contact.displayName);
        }

        h._emailButton.setColorFilter(_listener.colorForIcon(contact, ContactCardListener.IconType.ICON_EMAIL));
        h._smsButton.setColorFilter(_listener.colorForIcon(contact, ContactCardListener.IconType.ICON_SMS));
    }

    @Override
    public int getItemCount() {
        return _aylaContacts.size();
    }

    protected class ContactViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        public TextView _contactNameTextView;
        public ImageButton _emailButton;
        public ImageButton _smsButton;
        public ContactCardListener _listener;

        public ContactViewHolder(View v, ContactCardListener listener) {
            super(v);
            _listener = listener;
            v.setOnClickListener(this);
            v.setOnLongClickListener(this);
            _contactNameTextView = (TextView) v.findViewById(R.id.contact_name);
            _emailButton = (ImageButton)v.findViewById(R.id.button_email);
            _smsButton = (ImageButton)v.findViewById(R.id.button_sms);

            _emailButton.setOnClickListener(this);
            _smsButton.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            AylaContact contact = _aylaContacts.get(getPosition());
            if ( v == _emailButton ) {
                _listener.emailTapped(contact);
            } else if ( v == _smsButton ) {
                _listener.smsTapped(contact);
            } else {
                _listener.contactTapped(contact);
            }
        }

        @Override
        public boolean onLongClick(View v) {
            _listener.contactLongTapped(_aylaContacts.get(getPosition()));
            return true;
        }
    }
}
