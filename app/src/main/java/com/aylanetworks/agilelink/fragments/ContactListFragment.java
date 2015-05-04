package com.aylanetworks.agilelink.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import com.aylanetworks.aaml.AylaContact;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.fragments.adapters.ContactListAdapter;
import com.aylanetworks.agilelink.framework.ContactManager;
import com.aylanetworks.agilelink.framework.SessionManager;

/**
 * PropertyNotificationFragment.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 3/19/2015.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */
public class ContactListFragment extends Fragment implements View.OnClickListener, FragmentManager.OnBackStackChangedListener, ContactListAdapter.ContactCardListener {
    private final static String LOG_TAG = "ContactListFrag";

    private RecyclerView _recyclerView;
    private RecyclerView.LayoutManager _layoutManager;

    public static ContactListFragment newInstance() {
        return new ContactListFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        getFragmentManager().addOnBackStackChangedListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getFragmentManager().removeOnBackStackChangedListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onCreateView: " + savedInstanceState);

        // We re-use the "All Devices" fragment layout, as it's the same as we need
        View view = inflater.inflate(R.layout.fragment_all_devices, container, false);

        _recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        _recyclerView.setHasFixedSize(true);

        _layoutManager = new LinearLayoutManager(getActivity());
        _recyclerView.setLayoutManager(_layoutManager);

        ImageButton b = (ImageButton) view.findViewById(R.id.add_button);
        b.setOnClickListener(this);

        // Do a deep fetch of the contact list
        MainActivity.getInstance().showWaitDialog(R.string.fetching_contacts_title, R.string.fetching_contacts_body);

        ContactManager cm = SessionManager.getInstance().getContactManager();
        cm.fetchContacts(new ContactManager.ContactManagerListener() {
            @Override
            public void contactListUpdated(ContactManager manager, boolean succeeded) {
                MainActivity.getInstance().dismissWaitDialog();
                _recyclerView.setAdapter(new ContactListAdapter(false, ContactListFragment.this));
            }
        }, true);

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_add_contact, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch ( item.getItemId() ) {
             case R.id.action_fill_from_contact:
                // Push the edit contact fragment and set it as the delegate of the contact picker
                EditContactFragment frag = EditContactFragment.newInstance(null);
                MainActivity.getInstance().pushFragment(frag);
                MainActivity.getInstance().pickContact(frag);
                return true;
        }

        return false;
    }

    @Override
    public void onClick(View v) {
        Log.d(LOG_TAG, "Add button clicked");
        EditContactFragment frag = EditContactFragment.newInstance(null);

        // We want to handle our own navigation here
        getFragmentManager().beginTransaction().
                setCustomAnimations(R.anim.abc_fade_in, R.anim.abc_fade_out,
                        R.anim.abc_fade_in, R.anim.abc_fade_out)
                .add(android.R.id.content, frag)
                .addToBackStack(null).commit();
    }

    @Override
    public void onBackStackChanged() {
        Log.d(LOG_TAG, "Back stack changed");
        _recyclerView.setAdapter(new ContactListAdapter(false, this));
    }

    @Override
    public void emailTapped(AylaContact contact) {
        Log.d(LOG_TAG, "Email tapped: " + contact);
    }

    @Override
    public void smsTapped(AylaContact contact) {
        Log.d(LOG_TAG, "SMS tapped: " + contact);
    }

    @Override
    public void contactTapped(AylaContact contact) {
        Log.d(LOG_TAG, "Editing contact: " + contact);
        EditContactFragment frag = EditContactFragment.newInstance(contact);
        MainActivity.getInstance().pushFragment(frag);
    }

    @Override
    public void contactLongTapped(final AylaContact contact) {
        // Confirm delete
        String body = String.format(MainActivity.getInstance().getString(R.string.confirm_delete_contact_body),
                contact.displayName);

        new AlertDialog.Builder(MainActivity.getInstance())
                .setTitle(R.string.confirm_delete_contact_title)
                .setMessage(body)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteContactConfirmed(contact);
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .create().show();
    }

    @Override
    public int colorForIcon(AylaContact contact, IconType iconType) {
        switch ( iconType ) {
            case ICON_EMAIL:
                if ( !TextUtils.isEmpty(contact.email)) {
                    return MainActivity.getInstance().getResources().getColor(R.color.app_theme_accent);
                } else {
                    return MainActivity.getInstance().getResources().getColor(R.color.disabled_text);
                }

            case ICON_SMS:
                if ( !TextUtils.isEmpty(contact.phoneNumber)) {
                    return MainActivity.getInstance().getResources().getColor(R.color.app_theme_accent);
                } else {
                    return MainActivity.getInstance().getResources().getColor(R.color.disabled_text);
                }
        }
        // we should never get here...
        return MainActivity.getInstance().getResources().getColor(R.color.disabled_text);
    }

    private void deleteContactConfirmed(final AylaContact contact) {
        MainActivity.getInstance().showWaitDialog(R.string.deleting_contact_title, R.string.deleting_contact_body);
        SessionManager.getInstance().getContactManager().deleteContact(contact, new ContactManager.ContactManagerListener() {
            @Override
            public void contactListUpdated(ContactManager manager, boolean succeeded) {
                MainActivity.getInstance().dismissWaitDialog();
                if ( succeeded ) {
                    ContactListAdapter adapter = new ContactListAdapter(true, ContactListFragment.this);
                    _recyclerView.setAdapter(adapter);
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
}
