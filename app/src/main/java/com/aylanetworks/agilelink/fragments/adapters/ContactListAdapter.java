package com.aylanetworks.agilelink.fragments.adapters;

import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.aylanetworks.agilelink.framework.AMAPCore;
import com.aylanetworks.aylasdk.AylaContact;
import com.aylanetworks.agilelink.R;

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
        public enum IconType { ICON_PUSH, ICON_EMAIL, ICON_SMS };
        boolean isOwner(AylaContact contact);
        void pushTapped(AylaContact contact);
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
        _aylaContacts = AMAPCore.sharedInstance().getContactManager().getContacts(includeOwner);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_contact, parent, false);
        return new ContactViewHolder(v, _listener);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ContactViewHolder h = (ContactViewHolder) holder;
        h._contact = _aylaContacts.get(position);

        if (TextUtils.isEmpty(h._contact.getDisplayName()) ||
                TextUtils.isEmpty(h._contact.getDisplayName().trim()
        )) {
            if (TextUtils.isEmpty(h._contact.getFirstname()) &&
                    TextUtils.isEmpty(h._contact.getLastname())) {
                h._contactNameTextView.setText(h._contact.getEmail());
            } else {
                h._contactNameTextView.setText(h._contact.getFirstname() + " " +
                        h._contact.getLastname());
            }
        } else {
            h._contactNameTextView.setText(h._contact.getDisplayName());
        }

        h._isOwner = _listener.isOwner(h._contact);
        h._pushButton.setVisibility(h._isOwner ? View.VISIBLE : View.GONE);
        h._pushButton.setColorFilter(_listener.colorForIcon(h._contact, ContactCardListener.IconType.ICON_PUSH));
        h._emailButton.setColorFilter(_listener.colorForIcon(h._contact, ContactCardListener.IconType.ICON_EMAIL));
        h._smsButton.setColorFilter(_listener.colorForIcon(h._contact, ContactCardListener.IconType.ICON_SMS));
    }

    @Override
    public int getItemCount() {
        return _aylaContacts.size();
    }

    protected class ContactViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        public AylaContact _contact;
        public TextView _contactNameTextView;
        public ImageButton _pushButton;
        public ImageButton _emailButton;
        public ImageButton _smsButton;
        public ContactCardListener _listener;
        public boolean _isOwner;

        public ContactViewHolder(View v, ContactCardListener listener) {
            super(v);
            _listener = listener;
            v.setOnClickListener(this);
            v.setOnLongClickListener(this);
            _contactNameTextView = (TextView) v.findViewById(R.id.contact_name);

            _pushButton = (ImageButton)v.findViewById(R.id.button_push);
            _emailButton = (ImageButton)v.findViewById(R.id.button_email);
            _smsButton = (ImageButton)v.findViewById(R.id.button_sms);

            _pushButton.setOnClickListener(this);
            _emailButton.setOnClickListener(this);
            _smsButton.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            AylaContact contact = _aylaContacts.get(getPosition());
            switch (v.getId()) {
                case R.id.button_push:
                    _listener.pushTapped(contact);
                    break;

                case R.id.button_sms:
                    _listener.smsTapped(contact);
                    break;

                case R.id.button_email:
                    _listener.emailTapped(contact);
                    break;

                default:
                    _listener.contactTapped(contact);
                    break;
            }
        }

        @Override
        public boolean onLongClick(View v) {
            _listener.contactLongTapped(_aylaContacts.get(getPosition()));
            return true;
        }
    }
}
