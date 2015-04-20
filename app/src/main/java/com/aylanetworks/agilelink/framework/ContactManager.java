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

/**
 * The ContactManager class is used to organize, store and update a list of AylaContact objects.
 *
 * Contacts contain information about how somebody might be notified in the case of device events.
 * This information currently includes a person's email address and SMS phone number, as well as
 * identifying information such as first and last names, a display name, street address, etc.
 *
 * The {@link com.aylanetworks.agilelink.framework.SessionManager} creates a ContactManager after
 * the user logs in and initializes it with information retrieved from the server. This object
 * can be accessed via {@link com.aylanetworks.agilelink.framework.SessionManager#getContactManager()}.
 */
public class ContactManager {
    private final static String LOG_TAG = "ContactManager";

    public static class ContactManagerListener {
        public Message lastMessage;
        public void contactListUpdated(ContactManager manager, boolean succeeded) {}
    }

    /**
     * Default constructor
     */
    public ContactManager() {
        Log.d(LOG_TAG, "ContactManager created");
        _aylaContactList = new ArrayList<>();
    }

    /**
     * Returns the list of AylaContacts for the current user.
     * @param includeOwner If set to true, will include the owner contact in the list. Otherwise
     *                     the owner contact will not be included.
     * @return the list of AylaContacts for the current user.
     */
    public List<AylaContact>getContacts(boolean includeOwner) {
        List<AylaContact> contacts = new ArrayList<>(_aylaContactList);
        if ( !includeOwner ) {
            AylaContact ownerContact = getOwnerContact();
            if ( ownerContact != null ) {
                for (AylaContact c : contacts) {
                    if (c.id.equals(ownerContact.id) ) {
                        Log.d(LOG_TAG, "Removing contact: " + c);
                        contacts.remove(c);
                        break;
                    }
                }
            }
        }

        return contacts;
    }

    /**
     * Adds the provided AylaContact to the current user's account. The listener will be notified
     * when the contact has been updated on the server, or if an error occurred.
     * @param contact The AylaContact to add.
     * @param listener The listener to be notified when the contact has been added to the user's
     *                 account, or if an error occurrs.
     */
    public void addContact(AylaContact contact, ContactManagerListener listener) {
        contact.create(new ContactHandler(ContactHandler.Command.ADD, contact, this, listener), null);
    }

    /**
     * Returns the owner contact. The owner contact is the contact that represents the owner of the
     * account. This contact does not show up in the contact list, and is updated when the user's
     * profile is modified (within the app).
     * @return the owner contact, or null if not found.
     */
    @Nullable
    public AylaContact getOwnerContact() {
        AccountSettings settings = SessionManager.getInstance().getAccountSettings();
        if ( settings == null ) {
            Log.e(LOG_TAG, "Trying to get owner contact, but we have no account settings yet");
            return null;
        }

        Integer ownerID = settings.getOwnerContactID();
        if ( ownerID == null ) {
            return null;
        }

        return getContactByID(ownerID);
    }

    /**
     * Returns the contact with the specified contact ID
     * @param contactID The contact's ID
     * @return The contact with the specified contact ID, or null if not found.
     */
    @Nullable
    public AylaContact getContactByID(int contactID) {
        for ( AylaContact contact : _aylaContactList ) {
            if ( contact.id == contactID ) {
                return contact;
            }
        }
        return null;
    }

    /**
     * Updates the specified contact on the user's account.
     * @param contact The modified contact to update
     * @param listener Listener to be notified when the operation is complete
     */
    public void updateContact(AylaContact contact, ContactManagerListener listener) {
        Log.d(LOG_TAG, "updateContact: " + contact);
        contact.update(new ContactHandler(ContactHandler.Command.UPDATE, contact, this, listener));
    }

    /**
     * Deletes the specified contact on the user's account.
     * @param contact The contact to be removed
     * @param listener Listener to be notified when the operation is complete
     */
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

    /**
     * Helper method used to update an AylaContact with information from an AylaUser
     * @param contact The AylaContact to update
     * @param user The AylaUser to update from
     */
    public static void updateContactFromUser(AylaContact contact, AylaUser user) {
        contact.firstname = user.firstname;
        contact.lastname = user.lastname;

        // TODO: This may be different in other locales
        contact.displayName = user.firstname + " " + user.lastname;

        contact.country = user.country;
        contact.email = user.email;
        contact.phoneCountryCode = user.phoneCountryCode;
        contact.phoneNumber = user.phone;
        contact.streetAddress = user.street;
    }

    private List<AylaContact> _aylaContactList;

    /**
     * Creates the owner contact from the AylaUser returned from AylaUser.getCurrent(), and sets
     * the contact ID in the account settings datum.
     */
    public void createOwnerContact() {
        AylaUser user = AylaUser.getCurrent();
        AylaContact contact = new AylaContact();

        updateContactFromUser(contact, user);

        Log.d(LOG_TAG, "Adding owner contact: " + contact);
        contact.create(new ContactHandler(ContactHandler.Command.ADD_OWNER, contact, this, new ContactManagerListener()), null);
    }

    /**
     * Removes a contact from our internal list. The removal is done by iterating and comparing the
     * contact IDs, as the AylaContact class does not have an equals method.
     * @param contact contact to remove from the list
     * @return true if the contact was removed, false otherwise.
     */
    private boolean removeContact(AylaContact contact) {
        for ( AylaContact c : _aylaContactList ) {
            if ( contact.id.equals(c.id) ) {
                _aylaContactList.remove(c);
                return true;
            }
        }
        return false;
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
            if ( AylaNetworks.succeeded(msg) ) {
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
            if ( AylaNetworks.succeeded(msg) ) {
                // Replace our existing contact with the updated one
                AylaContact updatedContact = AylaSystemUtils.gson.fromJson((String)msg.obj, AylaContact.class);
                Log.d(LOG_TAG, "Updated contact: " + updatedContact);

                for ( AylaContact contact : _contactManager.get()._aylaContactList ) {
                    if ( contact.id == updatedContact.id ) {
                        Log.d(LOG_TAG, "Old contact found: " + contact);
                        _contactManager.get()._aylaContactList.remove(contact);
                        break;
                    }
                }
                AylaContact oldContact = _contactManager.get().getContactByID(updatedContact.id);
                if ( oldContact != null ) {
                    _contactManager.get().removeContact(oldContact);
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
            Log.d(LOG_TAG, "ContactHandler [" + _command.toString() + "] " + msg);

            List<AylaContact> contactList = _contactManager.get()._aylaContactList;
            if ( AylaNetworks.succeeded(msg) ) {
                Gson gson = AylaSystemUtils.gson;
                switch (_command) {
                    case ADD:
                    case ADD_OWNER:{
                        AylaContact newContact = gson.fromJson((String) msg.obj, AylaContact.class);
                        contactList.add(newContact);

                        if ( _command == Command.ADD_OWNER ) {
                            AccountSettings settings = SessionManager.getInstance().getAccountSettings();
                            if ( settings != null ) {
                                settings.setOwnerContactID(newContact.id);
                                settings.pushToServer(new AccountSettings.AccountSettingsCallback());
                                Log.d(LOG_TAG, "Owner contact ID: " + newContact.id);
                            } else {
                                Log.e(LOG_TAG, "Can't set owner contact ID without account settings");
                                msg.what = AylaNetworks.AML_ERROR_FAIL;
                            }
                        }
                    }
                    break;

                     case UPDATE: {
                        AylaContact updatedContact = gson.fromJson((String) msg.obj, AylaContact.class);
                        AylaContact oldContact = _contactManager.get().getContactByID(updatedContact.id);
                        if ( oldContact != null ) {
                            _contactManager.get().removeContact(oldContact);
                        }
                        contactList.add(updatedContact);
                    }
                    break;

                    case DELETE:
                        _contactManager.get().removeContact(_contact.get());
                    break;
                }
            }

            _listener.contactListUpdated(_contactManager.get(), AylaNetworks.succeeded(msg));
        }
    }
}
