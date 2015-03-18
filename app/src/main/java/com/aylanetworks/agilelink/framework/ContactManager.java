package com.aylanetworks.agilelink.framework;

import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

import com.aylanetworks.aaml.AylaContact;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.aaml.AylaUser;
import com.google.gson.Gson;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * ContactManager.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 3/18/2015
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class ContactManager {
    private final static String LOG_TAG = "ContactManager";

    public static class ContactManagerListener {
        public Message lastMessage;
        void contactListUpdated(ContactManager manager, boolean succeeded) {}
    }

    /**
     * Default constructor
     */
    public ContactManager() {
        Log.d(LOG_TAG, "ContactManager created");
        if ( SessionManager.getInstance().getAccountSettings() == null ) {
            throw new RuntimeException("Cannot create the contact manager before AccountSettings have been fetched");
        }
        _aylaContactList = new ArrayList<>();
    }

    public List<AylaContact>getContacts() {
        return new ArrayList<>(_aylaContactList);
    }

    public void addContact(AylaContact contact, ContactManagerListener listener) {
        contact.create(new ContactHandler(ContactHandler.Command.ADD, contact, this, listener), null);
    }

    @Nullable
    public AylaContact getOwnerContact() {
        Integer ownerID = SessionManager.getInstance().getAccountSettings().getOwnerContactID();
        if ( ownerID == null ) {
            return null;
        }

        return getContactByID(ownerID);
    }

    @Nullable
    public AylaContact getContactByID(int contactID) {
        for ( AylaContact contact : _aylaContactList ) {
            if ( contact.id == contactID ) {
                return contact;
            }
        }
        return null;
    }

    public void updateContact(AylaContact contact, ContactManagerListener listener) {
        contact.update(new ContactHandler(ContactHandler.Command.UPDATE, contact, this, listener));
    }

    public void deleteContact(AylaContact contact, ContactManagerListener listener) {
        contact.delete(new ContactHandler(ContactHandler.Command.DELETE, contact, this, listener));
    }

    /**
     * Fetches the contact list from the server. If deepFetch is true, will fetch all contact
     * details as well as the list. Otherwise only the list will be fetched.
     * @param listener Listener to be notified when the operation is complete
     * @param deepFetch Set to true to fetch all contact details, or false for the bare list
     */
    public void fetchContacts(ContactManagerListener listener, boolean deepFetch) {
        AylaContact.get(new FetchContactsHandler(this, listener, deepFetch), null);
    }

    private List<AylaContact> _aylaContactList;

    private void createOwnerContact() {
        AylaUser user = AylaUser.getCurrent();
        AylaContact contact = new AylaContact();

        contact.firstName = user.firstname;
        contact.lastName = user.lastname;

        // TODO: This may be different in other locales
        contact.displayName = user.firstname + " " + user.lastname;

        contact.country = user.country;
        contact.email = user.email;
        contact.phoneCountryCode = user.phoneCountryCode;
        contact.phoneNumber = user.phone;
        contact.streetAddress = user.street;

        Log.d(LOG_TAG, "Adding owner contact: " + contact);
        contact.create(new ContactHandler(ContactHandler.Command.ADD_OWNER, contact, this, new ContactManagerListener()), null);
    }


    private static class FetchContactsHandler extends Handler {
        private WeakReference<ContactManager> _contactManager;
        private ContactManagerListener _listener;
        private boolean _deepFetch;

        private List<AylaContact> _deepFetchList;

        public FetchContactsHandler(ContactManager contactManager, ContactManagerListener listener, boolean deepFetch) {
            _contactManager = new WeakReference<ContactManager>(contactManager);
            _listener = listener;
            _deepFetch = deepFetch;
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(LOG_TAG, "FetchContactsHandler: " + msg);
            if ( _deepFetchList != null ) {
                // We're fetching contact details.
                handleContactDetails(msg);
            } else {
                // We fetched the contact list
                handleContactList(msg);
            }
         }

        private void handleContactList(Message msg) {
            _listener.lastMessage = msg;
            if ( msg.what == AylaNetworks.AML_ERROR_OK ) {
                AylaContact[] contacts = AylaSystemUtils.gson.fromJson((String)msg.obj, AylaContact[].class);
                _contactManager.get()._aylaContactList = new ArrayList<AylaContact>(Arrays.asList(contacts));

                if ( _contactManager.get().getOwnerContact() == null ) {
                    _contactManager.get().createOwnerContact();
                }

                if ( _deepFetch ) {
                    _deepFetchList = new ArrayList<AylaContact>(_contactManager.get()._aylaContactList);
                    fetchNextContactDetails();
                } else {
                    // We're done.
                    _listener.contactListUpdated(_contactManager.get(), true);
                }
            } else {
                // Something went wrong. Notify the listener anyway.
                _listener.contactListUpdated(_contactManager.get(), false);
            }
        }

        private void handleContactDetails(Message msg) {
            if ( msg.what == AylaNetworks.AML_ERROR_OK ) {
                // Replace our existing contact with the updated one
                AylaContact updatedContact = AylaSystemUtils.gson.fromJson((String)msg.obj, AylaContact.class);
                for ( AylaContact contact : _contactManager.get()._aylaContactList ) {
                    if ( contact.id == updatedContact.id ) {
                        _contactManager.get()._aylaContactList.remove(contact);
                        break;
                    }
                }
                _contactManager.get()._aylaContactList.add(updatedContact);
                fetchNextContactDetails();
            }
        }

        private void fetchNextContactDetails() {
            if ( _deepFetchList.isEmpty() ) {
                // We're done.
                _deepFetchList = null;
                _listener.contactListUpdated(_contactManager.get(), true);
                return;
            }

            AylaContact contact = _deepFetchList.remove(0);
            Map<String, String> params = new HashMap<>();
            params.put(AylaContact.kAylaContactContactId, contact.id.toString());
            AylaContact.get(this, params);
            Log.d(LOG_TAG, "Fetching details for " + contact);
        }
    }

    /**
     * Generic handler for adding / updating / deleting contacts
     */
    private static class ContactHandler extends Handler {
        public enum Command {
            ADD,
            ADD_OWNER,
            REMOVE,
            UPDATE,
            DELETE
        }

        private Command _command;
        private WeakReference<AylaContact> _contact;
        private WeakReference<ContactManager>_contactManager;
        private ContactManagerListener _listener;

        public ContactHandler(Command command, AylaContact contact, ContactManager contactManager, ContactManagerListener listener) {
            _command = command;
            _contact = new WeakReference<AylaContact>(contact);
            _contactManager = new WeakReference<ContactManager>(contactManager);
            _listener = listener;
        }

        @Override
        public void handleMessage(Message msg) {
            _listener.lastMessage = msg;
            if ( msg.what == AylaNetworks.AML_ERROR_OK ) {
                Gson gson = AylaSystemUtils.gson;
                switch (_command) {
                    case ADD:
                    case ADD_OWNER:{
                        AylaContact newContact = gson.fromJson((String) msg.obj, AylaContact.class);
                        _contactManager.get()._aylaContactList.add(newContact);
                        if ( _command == Command.ADD_OWNER ) {
                            SessionManager.getInstance().getAccountSettings().setOwnerContactID(newContact.id);
                            SessionManager.getInstance().getAccountSettings().pushToServer(new AccountSettings.AccountSettingsCallback());
                            Log.d(LOG_TAG, "Owner contact ID: " + newContact.id);
                        }
                    }
                    break;

                    case REMOVE: {
                        _contactManager.get()._aylaContactList.remove(_contact.get());
                    }
                    break;

                    case UPDATE: {
                        AylaContact updatedContact = gson.fromJson((String) msg.obj, AylaContact.class);
                        AylaContact oldContact = _contactManager.get().getContactByID(updatedContact.id);
                        if ( oldContact != null ) {
                            _contactManager.get()._aylaContactList.remove(oldContact);
                        }
                        _contactManager.get()._aylaContactList.add(updatedContact);
                    }
                    break;

                    case DELETE: {
                        _contactManager.get()._aylaContactList.remove(_contact.get());
                    }
                    break;
                }
            }

            _listener.contactListUpdated(_contactManager.get(), msg.what == AylaNetworks.AML_ERROR_OK);
        }
    }
}
