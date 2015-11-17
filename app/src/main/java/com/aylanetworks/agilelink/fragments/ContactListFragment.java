package com.aylanetworks.agilelink.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Message;
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

import com.aylanetworks.aaml.AylaContact;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.fragments.adapters.ContactListAdapter;
import com.aylanetworks.agilelink.framework.AccountSettings;
import com.aylanetworks.agilelink.framework.ContactManager;
import com.aylanetworks.agilelink.framework.DeviceManager;
import com.aylanetworks.agilelink.framework.DeviceNotificationHelper;
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

        ContactManager cm = SessionManager.getInstance().getContactManager();
        _ownerContact = cm.getOwnerContact();
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
    public void contactTapped(AylaContact contact) {
        Log.d(LOG_TAG, "Editing contact: " + contact);
        MainActivity.getInstance().pushFragment(EditContactFragment.newInstance(contact));
    }

    @Override
    public boolean isOwner(AylaContact contact) {
        if ((contact == null) || (_ownerContact == null)) {
            return false;
        }
        if (contact.id == _ownerContact.id) {
            return true;
        }
        if (TextUtils.equals(contact.email, _ownerContact.email)) {
            return true;
        }
        return false;
    }

    boolean stateForIcon(AylaContact contact, IconType iconType) {
        AccountSettings accountSettings = SessionManager.getInstance().getAccountSettings();
        boolean isOwner = isOwner(contact);
        boolean checked = false;

        switch ( iconType ) {
            case ICON_PUSH: {
                if (accountSettings != null) {
                    checked = accountSettings.isNotificationMethodSet(DeviceNotificationHelper.NOTIFICATION_METHOD_PUSH);
                }
                break;
            }

            case ICON_EMAIL: {
                if (isOwner) {
                    if (accountSettings != null) {
                        checked = accountSettings.isNotificationMethodSet(DeviceNotificationHelper.NOTIFICATION_METHOD_EMAIL);
                    }
                } else {
                    checked = contact.emailNotification;
                }
                break;
            }

            case ICON_SMS: {
                if (isOwner) {
                    if (accountSettings != null) {
                        checked = accountSettings.isNotificationMethodSet(DeviceNotificationHelper.NOTIFICATION_METHOD_SMS);
                    }
                } else {
                    checked = contact.smsNotification;
                }
                break;
            }
        }
        return checked;
    }

    void enableNotification(final String notificationMethod, final boolean enable) {
        AccountSettings accountSettings = SessionManager.getInstance().getAccountSettings();
        if ( accountSettings == null ) {
            // Fetch the account settings now
            MainActivity.getInstance().showWaitDialog(R.string.fetching_account_info_title, R.string.fetching_account_info_body);
            SessionManager.getInstance().fetchAccountSettings(new AccountSettings.AccountSettingsCallback() {
                @Override
                public void settingsUpdated(AccountSettings settings, Message msg) {
                    MainActivity.getInstance().dismissWaitDialog();
                    if ( settings != null ) {
                        enableNotification(notificationMethod, enable);
                    } else {
                        Toast.makeText(MainActivity.getInstance(), R.string.unknown_error, Toast.LENGTH_SHORT).show();
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

    void updateNotifications(String notificationType, boolean enable) {
        SessionManager.deviceManager().updateDeviceNotifications(
                notificationType,
                enable,
                new DeviceManager.DeviceNotificationListener() {
                    @Override
                    public void notificationsUpdated(boolean succeeded, Message failureMessage) {
                        MainActivity.getInstance().dismissWaitDialog();
                        if (succeeded) {
                            Toast.makeText(getActivity(), R.string.notifications_updated, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getActivity(), R.string.notification_update_failed, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    void updateCheckboxes() {
        _recyclerView.getAdapter().notifyDataSetChanged();
    }

    class UpdateSettingsCallback extends AccountSettings.AccountSettingsCallback {
        private boolean _dismissDialogWhenDone;
        public UpdateSettingsCallback(boolean dismissDialogWhenDone) {
            _dismissDialogWhenDone = dismissDialogWhenDone;
        }
        @Override
        public void settingsUpdated(AccountSettings settings, Message msg) {
            updateCheckboxes();
            if ( _dismissDialogWhenDone ) {
                MainActivity.getInstance().dismissWaitDialog();
            }
        }
    }

    @Override
    public void pushTapped(AylaContact contact) {
        boolean isChecked = stateForIcon(contact, IconType.ICON_PUSH);
        Log.d(LOG_TAG, "contact: Push tapped " + contact.email + " - " + isChecked);
        enableNotification(DeviceNotificationHelper.NOTIFICATION_METHOD_PUSH, !isChecked);
    }

    @Override
    public void emailTapped(AylaContact contact) {
        boolean isOwner = isOwner(contact);
        boolean isChecked = stateForIcon(contact, IconType.ICON_EMAIL);
        Log.d(LOG_TAG, "contact: Email tapped " + contact.email + " - " + isChecked);
        if (isOwner) {
            enableNotification(DeviceNotificationHelper.NOTIFICATION_METHOD_EMAIL, !isChecked);
        } else{
            if(contact.email != null){
                if(!TextUtils.isEmpty(contact.email)){
                    contact.emailNotification = !isChecked;
                    MainActivity.getInstance().showWaitDialog(R.string.updating_contact_title, R.string.updating_contact_title);
                    SessionManager.getInstance().getContactManager().updateContact(contact, listener);
                }

            }

        }
    }

    final ContactManager.ContactManagerListener listener = new ContactManager.ContactManagerListener() {
        @Override
        public void contactListUpdated(ContactManager manager, boolean succeeded) {
            MainActivity.getInstance().dismissWaitDialog();
            if (succeeded) {
                Toast.makeText(getActivity(), R.string.contact_updated, Toast.LENGTH_LONG).show();
                _recyclerView.setAdapter(new ContactListAdapter(false, ContactListFragment.this));
            } else {
                if (lastMessage.obj != null) {
                    Toast.makeText(getActivity(), (String) lastMessage.obj, Toast.LENGTH_LONG).show();
                } else {
                    // Generic error message
                    Toast.makeText(getActivity(), R.string.contact_update_failed, Toast.LENGTH_LONG).show();
                }
            }
        }
    };
    @Override
    public void smsTapped(AylaContact contact) {
        boolean isOwner = isOwner(contact);
        boolean isChecked = stateForIcon(contact, IconType.ICON_SMS);
        Log.d(LOG_TAG, "contact: Sms tapped " + contact.email + " - " + isChecked);
        if (isOwner) {
            enableNotification(DeviceNotificationHelper.NOTIFICATION_METHOD_SMS, !isChecked);
        } else{
            //update contact
            if(contact.phoneNumber != null){
                if(!TextUtils.isEmpty(contact.phoneNumber)){
                    contact.smsNotification = !isChecked;
                    MainActivity.getInstance().showWaitDialog(R.string.updating_contact_title, R.string.updating_contact_title);
                    SessionManager.getInstance().getContactManager().updateContact(contact, listener);
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
        SessionManager.getInstance().getContactManager().deleteContact(contact, new ContactManager.ContactManagerListener() {
            @Override
            public void contactListUpdated(ContactManager manager, boolean succeeded) {
                MainActivity.getInstance().dismissWaitDialog();
                if ( succeeded ) {
                    ContactListAdapter adapter = new ContactListAdapter(false, ContactListFragment.this);
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
