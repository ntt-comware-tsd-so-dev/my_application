package com.aylanetworks.agilelink.fragments.adapters;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.aylanetworks.aaml.AylaContact;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.fragments.EditContactFragment;
import com.aylanetworks.agilelink.framework.ContactManager;
import com.aylanetworks.agilelink.framework.SessionManager;

import java.util.List;

/**
 * Created by Brian King on 3/18/15.
 */
public class ContactListAdapter extends RecyclerView.Adapter {
    private final static String LOG_TAG = "ContactListAdapter";

    private List<AylaContact> _aylaContacts;

    public ContactListAdapter() {
    }

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
    }

    @Override
    public int getItemCount() {
        return _aylaContacts.size();
    }

    public void editContact(int position) {
        AylaContact contact = _aylaContacts.get(position);
        Log.d(LOG_TAG, "Editing contact: " + contact);
        EditContactFragment frag = EditContactFragment.newInstance(contact);
        MainActivity.getInstance().pushFragment(frag);
    }

    public void deleteContact(final int position) {
        // Confirm delete
        AylaContact contact = _aylaContacts.get(position);

        String body = String.format(MainActivity.getInstance().getString(R.string.confirm_delete_contact_body),
                contact.displayName);

        new AlertDialog.Builder(MainActivity.getInstance())
                .setTitle(R.string.confirm_delete_contact_title)
                .setMessage(body)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteContactConfirmed(position);
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .create().show();
    }

    private void deleteContactConfirmed(final int position) {
        AylaContact contact = _aylaContacts.get(position);
        MainActivity.getInstance().showWaitDialog(R.string.deleting_contact_title, R.string.deleting_contact_body);
        SessionManager.getInstance().getContactManager().deleteContact(contact, new ContactManager.ContactManagerListener() {
            @Override
            public void contactListUpdated(ContactManager manager, boolean succeeded) {
                MainActivity.getInstance().dismissWaitDialog();
                if ( succeeded ) {
                    _aylaContacts.remove(position);
                    notifyItemRemoved(position);
                } else {
                    String message;
                    if ( lastMessage.obj != null ) {
                        message = (String)lastMessage.obj;
                    } else {
                        message = MainActivity.getInstance().getString(R.string.unknown_error);
                    }
                    Toast.makeText(MainActivity.getInstance(), (String)message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private class ContactViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        public TextView _contactNameTextView;

        public ContactViewHolder(View v) {
            super(v);

            v.setOnClickListener(this);
            v.setOnLongClickListener(this);
            _contactNameTextView = (TextView) v.findViewById(R.id.contact_name);
        }

        @Override
        public void onClick(View v) {
            editContact(getPosition());
        }

        @Override
        public boolean onLongClick(View v) {
            deleteContact(getPosition());
            return true;
        }
    }
}
