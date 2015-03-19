package com.aylanetworks.agilelink.fragments.adapters;

import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.aylanetworks.aaml.AylaContact;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.SessionManager;

import java.util.List;

/**
 * Created by Brian King on 3/18/15.
 */
public class ContactListAdapter extends RecyclerView.Adapter {
    private final static String LOG_TAG = "ContactListAdapter";

    private List<AylaContact> _aylaContacts;

    public ContactListAdapter() {}

    public ContactListAdapter(boolean includeOwner) {
        _aylaContacts = SessionManager.getInstance().getContactManager().getContacts(includeOwner);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_contact, parent, false);
        return new ContactViewHolder(v);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ContactViewHolder h = (ContactViewHolder)holder;
        AylaContact contact = _aylaContacts.get(position);

        if (TextUtils.isEmpty(contact.displayName) || TextUtils.isEmpty(contact.displayName.trim())) {
            if ( TextUtils.isEmpty(contact.firstName) && TextUtils.isEmpty(contact.lastName)) {
                h._contactNameTextView.setText(contact.email);
            } else {
                h._contactNameTextView.setText(contact.firstName + " " + contact.lastName);
            }
        } else {
            h._contactNameTextView.setText(contact.displayName);
        }
    }

    @Override
    public int getItemCount() {
        return _aylaContacts.size();
    }

    public void editContact(int position) {
        AylaContact contact = _aylaContacts.get(position);
        Log.d(LOG_TAG, "Editing contact: " + contact);
    }

    private class ContactViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        public TextView _contactNameTextView;

        public ContactViewHolder(View v) {
            super(v);

            v.setOnClickListener(this);
            v.setOnLongClickListener(this);
            _contactNameTextView = (TextView)v.findViewById(R.id.contact_name);
        }

        @Override
        public void onClick(View v) {
            editContact(getPosition());
        }

        @Override
        public boolean onLongClick(View v) {
            // Bring up a menu
            Log.d(LOG_TAG, "onLongClick: " + getPosition());
            return true;
        }
    }
}
