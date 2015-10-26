package com.aylanetworks.agilelink.framework;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.aylanetworks.aaml.AylaApplicationTrigger;
import com.aylanetworks.aaml.AylaContact;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaProperty;
import com.aylanetworks.aaml.AylaPropertyTrigger;
import com.aylanetworks.aaml.AylaSystemUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * PropertyNotificationHelper.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 5/7/2015.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */
public class PropertyNotificationHelper {
    private static final String LOG_TAG = "PropHelper";

    private Device _device;
    private List<AylaProperty> _triggersToFetch;
    private List<AylaPropertyTrigger> _appTriggersToFetch;

    private AylaProperty _property;
    private AylaPropertyTrigger _propertyTrigger;
    private List<AylaContact> _pushContacts;
    private List<AylaContact> _emailContacts;
    private List<AylaContact> _smsContacts;

    private Device.FetchNotificationsListener _fetchNotificationsListener;
    private SetNotificationListener _setNotificationListener;

    public PropertyNotificationHelper(Device device) {
        _device = device;
    }

    /**
     * Fetches the AylaPropertyTriggers and their AylaTriggerApps
     * @param listener Listener to receive results
     */
    public void fetchNotifications(Device.FetchNotificationsListener listener) {
        // First we need to make sure we have all of the properties for this device
        _fetchNotificationsListener = listener;
        _triggersToFetch = new ArrayList<>();
        _appTriggersToFetch = new ArrayList<>();

        for (String propName : _device.getNotifiablePropertyNames()) {
            AylaProperty prop = _device.getProperty(propName);
            if (prop == null) {
                Log.e(LOG_TAG, "No property found: " + propName);
                // We'll just continue...
            } else {
                _triggersToFetch.add(prop);
            }
        }

        fetchNextTrigger();
    }

    public static abstract class SetNotificationListener {
        /** Set to the last message received from the server */
        public Message _lastMessage;

        /**
         * Called in response to {@link #setNotifications}
         * @param property Property notifications were set on
         * @param propertyTrigger Property trigger used for the notification
         * @param succeeded true if successful, false otherwise. See {@link #_lastMessage} for details.
         */
        public abstract void notificationsSet(AylaProperty property, AylaPropertyTrigger propertyTrigger, boolean succeeded);
    }

    /**
     * Creates or updates notifications for the given property for the given lists of contacts.
     *
     * @param property Property the triggers should be set on
     * @param propertyTrigger Property trigger to create. Will always create a new trigger on the service.
     * @param emailContacts List of contacts to be notified via email when the trigger fires
     * @param smsContacts List of contacts to be notified via SMS when the trigger fires
     * @param listener Listener to be notified when the operation is complete
     */
    public void setNotifications(AylaProperty property,
                                 AylaPropertyTrigger propertyTrigger,
                                 List<AylaContact> pushContacts,
                                 List<AylaContact> emailContacts,
                                 List<AylaContact> smsContacts,
                                 SetNotificationListener listener) {
        Log.d(LOG_TAG, "setNotifications: " + propertyTrigger);

        _property = property;
        _propertyTrigger = propertyTrigger;
        _setNotificationListener = listener;
        _pushContacts = new ArrayList<>(pushContacts);
        _emailContacts = new ArrayList<>(emailContacts);
        _smsContacts = new ArrayList<>(smsContacts);

        property.createTrigger(new PropertyTriggerHandler(this), propertyTrigger);
    }

    private void fetchNextTrigger() {
        if ( _triggersToFetch.isEmpty() ) {
            // Done fetching triggers.
            Log.d(LOG_TAG, "Done fetching triggers, moving on to app triggers");
            fetchNextAppTrigger();
        } else {
            AylaProperty prop = _triggersToFetch.remove(0);
            prop.getTriggers(new FetchTriggersHandler(this, prop), null);
        }

    }

    private void fetchNextAppTrigger() {
        if ( _appTriggersToFetch.isEmpty() ) {
            Log.d(LOG_TAG, "Finished fetching app triggers.");
            _fetchNotificationsListener.notificationsFetched(_device, true);
        } else {
            AylaPropertyTrigger trigger = _appTriggersToFetch.remove(0);
            // For some reason this is not static...
            AylaApplicationTrigger junk = new AylaApplicationTrigger();
            junk.getTriggers(new AppTriggerHandler(this, trigger), trigger, null);
        }
    }

    private void setNextTriggerApp() {
        AylaContact nextContact = null;
        if (!_pushContacts.isEmpty()) {
            nextContact = _pushContacts.remove(0);
            addTriggerApp(nextContact, true, false);
        } else if ( !_emailContacts.isEmpty() ) {
            nextContact = _emailContacts.remove(0);
            addTriggerApp(nextContact, false, true);
        } else if ( !_smsContacts.isEmpty() ) {
            nextContact = _smsContacts.remove(0);
            addTriggerApp(nextContact, false, false);
        } else  {
            // We're done!
            _setNotificationListener.notificationsSet(_property, _propertyTrigger, true);
        }
    }

    private void addTriggerApp(AylaContact contact, boolean isPush, boolean isEmail) {
        AylaApplicationTrigger app = new AylaApplicationTrigger();
        app.contactId = contact.id.toString();
        app.message = _device.friendlyNameForPropertyName(_property.name());
        if (isPush) {
            _propertyTrigger.createPushApplicationTrigger(new CreateAppTriggerHandler(this), app);
        } else if ( isEmail ) {
            app.emailAddress = contact.email;
            _propertyTrigger.createEmailApplicationTrigger(new CreateAppTriggerHandler(this), app);
        } else {
            app.phoneNumber = contact.phoneNumber;
            app.countryCode = contact.phoneCountryCode;
            _propertyTrigger.createSMSApplicationTrigger(new CreateAppTriggerHandler(this), app);
        }
    }

    /**
     * Handler for getAppTrigger
     */
    private static class AppTriggerHandler extends Handler {
        PropertyNotificationHelper _helper;
        AylaPropertyTrigger _propTrigger;

        public AppTriggerHandler(PropertyNotificationHelper helper, AylaPropertyTrigger propTrigger) {
            _helper = helper;
            _propTrigger = propTrigger;
        }

        @Override
        public void handleMessage(Message msg) {
            _helper._fetchNotificationsListener._lastMessage = msg;
            if ( AylaNetworks.succeeded(msg) ) {
                _propTrigger.applicationTriggers = AylaSystemUtils.gson.fromJson((String)msg.obj, AylaApplicationTrigger[].class);
                _helper.fetchNextAppTrigger();
            } else {
                Log.e(LOG_TAG, "Failed to fetch app triggers");
                _helper._fetchNotificationsListener.notificationsFetched(_helper._device, false);
            }
        }
    }

    /**
     * Handler for createAppTrigger()
     */
    private static class CreateAppTriggerHandler extends Handler {
        private PropertyNotificationHelper _helper;

        public CreateAppTriggerHandler(PropertyNotificationHelper helper) {
            _helper = helper;
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(LOG_TAG, "Create app trigger: " + msg);
            _helper._setNotificationListener._lastMessage = msg;
            if ( AylaNetworks.succeeded(msg)) {
                _helper.setNextTriggerApp();
            } else {
                Log.e(LOG_TAG, "Create app trigger failed!");
                _helper._setNotificationListener.notificationsSet(_helper._property, _helper._propertyTrigger, false);
            }
        }
    }

    /**
     * Handler for create / update of property triggers
     */
    private static class PropertyTriggerHandler extends Handler {
        private PropertyNotificationHelper _helper;

        public PropertyTriggerHandler(PropertyNotificationHelper helper) {
            _helper = helper;
        }

        @Override
        public void handleMessage(Message msg) {
            _helper._setNotificationListener._lastMessage = msg;
            if ( AylaNetworks.succeeded(msg)) {
                _helper._propertyTrigger = AylaSystemUtils.gson.fromJson((String)msg.obj, AylaPropertyTrigger.class);
                _helper.setNextTriggerApp();
            } else {
                Log.e(LOG_TAG, "Failed to set / update property trigger: " + msg);
                _helper._setNotificationListener.notificationsSet(_helper._property, _helper._propertyTrigger, false);
            }
        }
    }

    /**
     * Handler for getTriggers()
     */
    private static class FetchTriggersHandler extends Handler {
        private AylaProperty _property;
        private PropertyNotificationHelper _helper;

        public FetchTriggersHandler(PropertyNotificationHelper helper, AylaProperty property) {
            _property = property;
            _helper = helper;
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(LOG_TAG, "Property triggers: " + msg);
            _helper._fetchNotificationsListener._lastMessage = msg;
            if (AylaNetworks.succeeded(msg)) {
                _property.propertyTriggers = AylaSystemUtils.gson.fromJson((String)msg.obj, AylaPropertyTrigger[].class);

                // Save these off so that we can fetch the app triggers later
                if ( _property.propertyTriggers != null && _property.propertyTriggers.length > 0 ) {
                    _helper._appTriggersToFetch.addAll(Arrays.asList(_property.propertyTriggers));
                }
                _helper.fetchNextTrigger();
            } else {
                // Failed to get the trigger
                Log.e(LOG_TAG, "Failed to get triggers for " + _property);
                _helper._fetchNotificationsListener.notificationsFetched(_helper._device, false);
            }
        }
    }
}
