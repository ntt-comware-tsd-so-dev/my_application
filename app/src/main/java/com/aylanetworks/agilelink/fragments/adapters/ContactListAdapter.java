package com.aylanetworks.agilelink.fragments.adapters;

import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aylanetworks.aaml.AylaContact;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.SessionManager;

import java.util.List;

/**
 * Created by Brian King on 3/18/15.
 */
public class ContactListAdapter extends RecyclerView.Adapter {
    private List<AylaContact> _aylaContacts;

    public ContactListAdapter() {
        _aylaContacts = SessionManager.getInstance().getContactManager().getContacts();
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
}
