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
import android.widget.Toast;

import com.aylanetworks.agilelink.ErrorUtils;
import com.aylanetworks.agilelink.framework.AMAPCore;
import com.aylanetworks.aylasdk.AylaContact;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.fragments.adapters.ContactListAdapter;
import com.aylanetworks.agilelink.framework.AccountSettings;
import com.aylanetworks.agilelink.framework.ContactManager;
import com.aylanetworks.aylasdk.AylaServiceApp;
import com.aylanetworks.aylasdk.error.AylaError;

/**
 * PropertyNotificationFragment.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 3/19/2015.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */
public class ContactListFragment extends Fragment implements View.OnClickListener,
        FragmentManager.OnBackStackChangedListener,
        ContactListAdapter.ContactCardListener {
    private final static String LOG_TAG = "ContactListFrag";

    RecyclerView _recyclerView;
    RecyclerView.LayoutManager _layoutManager;
    AylaContact _ownerContact;

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

        view.findViewById(R.id.add_button).setOnClickListener(this);

        // Do a deep fetch of the contact list
        MainActivity.getInstance().showWaitDialog(R.string.fetching_contacts_title, R.string.fetching_contacts_body);

        ContactManager cm = AMAPCore.sharedInstance().getContactManager();
        _ownerContact = cm.getOwnerContact();

        cm.fetchContacts(new ContactManager.ContactManagerListener() {
            @Override
            public void contactListUpdated(ContactManager manager, AylaError error) {
                MainActivity.getInstance().dismissWaitDialog();
                _recyclerView.setAdapter(new ContactListAdapter(false, ContactListFragment.this));
            }
        });

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
            case R.id.action_add_contact:
                onAddClicked();
                return true;

            case R.id.action_fill_from_contact:
                // Push the edit contact fragment and set it as the delegate of the contact picker
                EditContactFragment frag = EditContactFragment.newInstance(null);
                MainActivity.getInstance().pushFragment(frag);
                MainActivity.getInstance().pickContact(frag);
                return true;
        }

        return false;
    }

    void onAddClicked() {
        Log.d(LOG_TAG, "Add button clicked");
        MainActivity.getInstance().pushFragment(EditContactFragment.newInstance(null));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.add_button:
                onAddClicked();
                break;
        }
    }

    @Override
    public void onBackStackChanged() {
        Log.d(LOG_TAG, "Back stack changed");
        _recyclerView.setAdapter(new ContactListAdapter(false, this));
    }

    @Override
    public void contactLongTapped(final AylaContact contact) {
        // Confirm delete
        String body = String.format(MainActivity.getInstance().getString(R.string.confirm_delete_contact_body),
                contact.getDisplayName());

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
    public void contactTapped(AylaContact contact) {
        Log.d(LOG_TAG, "Editing contact: " + contact);
        MainActivity.getInstance().pushFragment(EditContactFragment.newInstance(contact));
    }

    @Override
    public boolean isOwner(AylaContact contact) {
        if ((contact == null) || (_ownerContact == null)) {
            return false;
        }
        if (contact.getId() == _ownerContact.getId()) {
            return true;
        }
        if (TextUtils.equals(contact.getEmail(), _ownerContact.getEmail())) {
            return true;
        }
        return false;
    }

    private boolean stateForIcon(AylaContact contact, IconType iconType) {
        AccountSettings accountSettings = AMAPCore.sharedInstance().getAccountSettings();
        boolean isOwner = isOwner(contact);
        boolean checked = false;

        switch (iconType) {
            case ICON_EMAIL:
                if (isOwner) {
                    if (accountSettings != null) {
                        checked = accountSettings.isNotificationMethodSet(AylaServiceApp.NotificationType.EMail);
                    }
                } else {
                    checked = contact.getWantsEmailNotification();
                }
                break;

            case ICON_SMS:
                if (isOwner) {
                    if (accountSettings != null) {
                        checked = accountSettings.isNotificationMethodSet(AylaServiceApp.NotificationType.SMS);
                    }
                } else {
                    checked = contact.getWantsSmsNotification();
                }
                break;
        }

        return checked;
    }

    private void enableNotification(final AylaServiceApp.NotificationType notificationMethod, final boolean enable) {
        AccountSettings accountSettings = AMAPCore.sharedInstance().getAccountSettings();
        if ( accountSettings == null ) {
            // Fetch the account settings now
            MainActivity.getInstance().showWaitDialog(R.string.fetching_account_info_title, R.string.fetching_account_info_body);
            AMAPCore.sharedInstance().fetchAccountSettings(new AccountSettings.AccountSettingsCallback() {
                @Override
                public void settingsUpdated(AccountSettings settings, AylaError error) {
                    MainActivity.getInstance().dismissWaitDialog();
                    if ( settings != null ) {
                        enableNotification(notificationMethod, enable);
                    } else {
                        MainActivity.getInstance().dismissWaitDialog();
                        Toast.makeText(getActivity(),
                                ErrorUtils.getUserMessage(getActivity(), error, R.string.unknown_error),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
            return;
        }
        if ( enable ) {
            accountSettings.addNotificationMethod(notificationMethod);
        } else {
            accountSettings.removeNotificationMethod(notificationMethod);
        }
        MainActivity.getInstance().showWaitDialog(R.string.updating_notifications_title, R.string.updating_notifications_body);
        accountSettings.pushToServer(new UpdateSettingsCallback(false));
        updateNotifications(notificationMethod, enable);
    }

    private void updateNotifications(AylaServiceApp.NotificationType notificationType, boolean enable) {
        AMAPCore.sharedInstance().updateDeviceNotifications(
                notificationType,
                enable,
                new AMAPCore.DeviceNotificationListener() {
                    @Override
                    public void notificationsUpdated(AylaError error) {
                        MainActivity.getInstance().dismissWaitDialog();
                        if (error == null) {
                            Toast.makeText(getActivity(), R.string.notifications_updated, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getActivity(),
                                    ErrorUtils.getUserMessage(getActivity(), error, R.string.notification_update_failed),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void updateCheckboxes() {
        _recyclerView.setAdapter(new ContactListAdapter(false, ContactListFragment.this));
    }

    private class UpdateSettingsCallback extends AccountSettings.AccountSettingsCallback {
        private boolean _dismissDialogWhenDone;
        public UpdateSettingsCallback(boolean dismissDialogWhenDone) {
            _dismissDialogWhenDone = dismissDialogWhenDone;
        }
        @Override
        public void settingsUpdated(AccountSettings settings, AylaError error) {
            updateCheckboxes();
            if ( _dismissDialogWhenDone ) {
                MainActivity.getInstance().dismissWaitDialog();
            }
        }
    }

    @Override
    public void emailTapped(AylaContact contact) {
        boolean isOwner = isOwner(contact);
        boolean isChecked = stateForIcon(contact, IconType.ICON_EMAIL);
        Log.d(LOG_TAG, "contact: Email tapped " + contact.getEmail() + " - " + isChecked);

        if (isOwner) {
            enableNotification(AylaServiceApp.NotificationType.EMail, !isChecked);
        } else{
            if(contact.getEmail() != null){
                if(!TextUtils.isEmpty(contact.getEmail())){
                    contact.setWantsEmailNotification(!isChecked);
                    MainActivity.getInstance().showWaitDialog(R.string.updating_contact_title, R.string.updating_contact_title);
                    AMAPCore.sharedInstance().getContactManager().updateContact(contact, listener);
                }

            }

        }
    }

    final ContactManager.ContactManagerListener listener = new ContactManager.ContactManagerListener() {
        @Override
        public void contactListUpdated(ContactManager manager, AylaError error) {
            MainActivity.getInstance().dismissWaitDialog();
            if (error == null) {
                Toast.makeText(getActivity(), R.string.contact_updated, Toast.LENGTH_LONG).show();
                _recyclerView.setAdapter(new ContactListAdapter(false, ContactListFragment.this));
            } else {
                Toast.makeText(getActivity(),
                        ErrorUtils.getUserMessage(getActivity(), error, R.string.contact_update_failed),
                        Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    public void smsTapped(AylaContact contact) {
        boolean isOwner = isOwner(contact);
        boolean isChecked = stateForIcon(contact, IconType.ICON_SMS);
        Log.d(LOG_TAG, "contact: Sms tapped " + contact.getEmail() + " - " + isChecked);

        if (isOwner) {
            enableNotification(AylaServiceApp.NotificationType.SMS, !isChecked);
        } else{
            //update contact
            if(contact.getPhoneNumber() != null){
                if(!TextUtils.isEmpty(contact.getPhoneNumber())){
                    contact.setWantsSmsNotification(!isChecked);
                    MainActivity.getInstance().showWaitDialog(R.string.updating_contact_title, R.string.updating_contact_title);
                    AMAPCore.sharedInstance().getContactManager().updateContact(contact, listener);
                }
            }
        }
    }

    @Override
    public int colorForIcon(AylaContact contact, IconType iconType) {
        if (stateForIcon(contact, iconType)) {
            return MainActivity.getInstance().getResources().getColor(R.color.app_theme_accent);
        } else {
            return MainActivity.getInstance().getResources().getColor(R.color.disabled_text);
        }
    }

    private void deleteContactConfirmed(final AylaContact contact) {
        MainActivity.getInstance().showWaitDialog(R.string.deleting_contact_title, R.string.deleting_contact_body);
        AMAPCore.sharedInstance().getContactManager().deleteContact(contact, new ContactManager.ContactManagerListener() {
            @Override
            public void contactListUpdated(ContactManager manager, AylaError error) {
                MainActivity.getInstance().dismissWaitDialog();
                if ( error == null ) {
                    ContactListAdapter adapter = new ContactListAdapter(false, ContactListFragment.this);
                    _recyclerView.setAdapter(adapter);
                } else {
                    Toast.makeText(MainActivity.getInstance(),
                            ErrorUtils.getUserMessage(getActivity(), error, R.string.unknown_error),
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
