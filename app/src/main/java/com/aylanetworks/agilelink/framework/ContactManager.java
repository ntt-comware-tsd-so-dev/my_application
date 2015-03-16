package com.aylanetworks.agilelink.framework;

import android.os.Handler;

import com.aylanetworks.aaml.AylaContact;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Brian King on 3/13/15.
 */
public class ContactManager {

    public class ContactManagerListener {
        void contactListUpdated(ContactManager manager) {}
    }

    /**
     * Default constructor
     */
    public ContactManager() {
        _aylaContactList = new ArrayList<>();
    }

    public List<AylaContact>getContacts() {
        return new ArrayList<>(_aylaContactList);
    }

    public void addContact(AylaContact contact) {

    }

    public AylaContact getOwnerContact() {

    }

    public void updateContact(AylaContact contact) {

    }

    public void deleteContact(AylaContact contact, ) {

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

    private static class FetchContactsHandler extends Handler {
        public FetchContactsHandler(ContactManager contactManager, ContactManagerListener listener, boolean deepFetch) {
            _listener = listener;
            _deepFetch = deepFetch;
        }
    }
}
