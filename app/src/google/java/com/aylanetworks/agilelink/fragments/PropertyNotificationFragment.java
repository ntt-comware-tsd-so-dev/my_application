package com.aylanetworks.agilelink.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.PushProvider;
import com.aylanetworks.agilelink.framework.AMAPCore;
import com.aylanetworks.agilelink.framework.PushNotification;
import com.aylanetworks.agilelink.framework.ViewModel;
import com.aylanetworks.aylasdk.AylaAPIRequest;
import com.aylanetworks.aylasdk.AylaContact;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaLog;
import com.aylanetworks.aylasdk.AylaProperty;
import com.aylanetworks.aylasdk.AylaPropertyTrigger;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.fragments.adapters.ContactListAdapter;
import com.aylanetworks.agilelink.framework.AccountSettings;
import com.aylanetworks.agilelink.framework.ContactManager;
import com.aylanetworks.agilelink.framework.PropertyNotificationHelper;
import com.aylanetworks.aylasdk.AylaPropertyTriggerApp;
import com.aylanetworks.aylasdk.AylaServiceApp;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;

import java.util.ArrayList;
import java.util.List;

/**
 * PropertyNotificationFragment.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 5/4/2015.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */
public class PropertyNotificationFragment extends Fragment implements ContactListAdapter.ContactCardListener, View.OnClickListener, AdapterView.OnItemSelectedListener {
    private static final String ARG_DSN = "dsn";
    private static final String ARG_TRIGGER = "trigger";
    
    public static final String TRIGGER_COMPARE_ABSOLUTE = "compare_absolute";
    public static final String TRIGGER_ALWAYS = "always";

    private static final String LOG_TAG = "PropNotifFrag";
    private List<AylaContact> _pushContacts;
    private List<AylaContact> _emailContacts;
    private List<AylaContact> _smsContacts;
    private Spinner _propertySpinner;
    private EditText _nameEditText;
    private EditText _numberEditText;
    private RadioGroup _numberRadioGroup;
    private RadioGroup _booleanRadioGroup;
    private RadioGroup _motionRadioGroup;
    private String _originalTriggerName;
    private AylaPropertyTrigger _originalTrigger;
    private PropertyNotificationHelper _propertyNotificationHelper;
    private CheckBox _sendPushCheckbox;

    // Layouts for each of the property base types. We will enable the appropriate layout
    // when the property is selected.
    private LinearLayout _booleanLayout;
    private LinearLayout _integerLayout;
    private LinearLayout _motionSensorLayout;

    public static PropertyNotificationFragment newInstance(AylaDevice device) {
        return newInstance(device, null);
    }

    public static PropertyNotificationFragment newInstance(AylaDevice device, AylaPropertyTrigger triggerToEdit) {
        PropertyNotificationFragment frag = new PropertyNotificationFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DSN, device.getDsn());
        if ( triggerToEdit != null ) {
            args.putString(ARG_TRIGGER, triggerToEdit.getDeviceNickname());
        }
        frag.setArguments(args);

        return frag;
    }

    // Default constructor
    public PropertyNotificationFragment() {
        _pushContacts = new ArrayList<>();
        _emailContacts = new ArrayList<>();
        _smsContacts = new ArrayList<>();
    }

    private ViewModel _deviceModel;
    private AylaContact _ownerContact;

    private RecyclerView _recyclerView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        _deviceModel = null;
        if (getArguments() != null ) {
            String dsn = getArguments().getString(ARG_DSN);
            AylaDevice device = AMAPCore.sharedInstance().getDeviceManager()
                    .deviceWithDSN(getArguments().getString(ARG_DSN));
            _deviceModel = AMAPCore.sharedInstance().getSessionParameters().viewModelProvider
                    .viewModelForDevice(device);

            Log.d(LOG_TAG, "My device: " + device);

            _propertyNotificationHelper = new PropertyNotificationHelper(device);

            _originalTriggerName = getArguments().getString(ARG_TRIGGER);
            if ( _originalTriggerName != null ) {
                // Try to find the trigger
                for (AylaProperty prop : device.getProperties()) {
                    prop.fetchTriggers(
                            new Response.Listener<AylaPropertyTrigger[]>() {
                                @Override
                                public void onResponse(AylaPropertyTrigger[] response) {
                                    if (response != null && response.length > 0) {
                                        for (AylaPropertyTrigger trigger : response) {
                                            if (_originalTriggerName.equals
                                                    (trigger.getDeviceNickname())) {
                                                _originalTrigger = trigger;
                                                updateUI();
                                                break;
                                            }
                                        }
                                    }
                                }
                            },
                            new ErrorListener() {
                                @Override
                                public void onErrorResponse(AylaError error) {
                                    Toast.makeText(MainActivity.getInstance(), error.toString(),
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                }
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_property_notification, container, false);

        _recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        _recyclerView.setLayoutManager(layoutManager);
        _nameEditText = (EditText)view.findViewById(R.id.notification_name);
        _propertySpinner = (Spinner)view.findViewById(R.id.property_spinner);
        _booleanLayout = (LinearLayout)view.findViewById(R.id.layout_boolean);
        _integerLayout = (LinearLayout)view.findViewById(R.id.layout_integer);
        _motionSensorLayout = (LinearLayout)view.findViewById(R.id.layout_motion);
        _numberEditText = (EditText)view.findViewById(R.id.number_edit_text);
        _sendPushCheckbox = (CheckBox) view.findViewById(R.id.send_push_notifications);
        _sendPushCheckbox.setChecked(false);
        _sendPushCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    _pushContacts.add(_ownerContact);
                } else {
                    _pushContacts.remove(_ownerContact);
                }
            }
        });


        // Set up a listener to show / hide the numerical input field based on the selection
        _numberRadioGroup = (RadioGroup)view.findViewById(R.id.radio_group_integer);
        _numberRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if ( checkedId == R.id.radio_integer_changes ) {
                    _numberEditText.setVisibility(View.GONE);
                } else {
                    _numberEditText.setVisibility(View.VISIBLE);
                }
            }
        });

        _booleanRadioGroup = (RadioGroup)view.findViewById(R.id.radio_group_boolean);
        _motionRadioGroup = (RadioGroup)view.findViewById(R.id.radio_group_motion);

        String propertyNames[] = _deviceModel.getNotifiablePropertyNames();
        String friendlyNames[] = new String[propertyNames.length];
        for ( int i = 0; i < propertyNames.length; i++ ) {
            friendlyNames[i] = _deviceModel.friendlyNameForPropertyName(propertyNames[i]);
        }

        _propertySpinner.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, friendlyNames));
        _propertySpinner.setOnItemSelectedListener(this);
        view.findViewById(R.id.save_notifications).setOnClickListener(this);

        final TextView emptyView = (TextView)view.findViewById(R.id.empty);

        // Fetch the list of contacts, including the owner contact, and show them in the list
        ContactManager cm = AMAPCore.sharedInstance().getContactManager();
        _ownerContact = cm.getOwnerContact();
        ContactManager.ContactManagerListener listener = new ContactManager.ContactManagerListener() {
            @Override
            public void contactListUpdated(ContactManager manager, AylaError error) {
                _recyclerView.setAdapter(new ContactListAdapter(true, PropertyNotificationFragment
                        .this));
                emptyView.setVisibility(View.INVISIBLE);
                _recyclerView.setVisibility(View.VISIBLE);
            }
        };

        if ( cm.getContacts(false).isEmpty() ) {
            cm.fetchContacts(listener);
        } else {
            listener.contactListUpdated(cm, null);
        }

        if ( _propertySpinner.getCount() > 0 ) {
            _propertySpinner.setSelection(0);
        } else {
            Toast.makeText(getActivity(), R.string.notifcations_not_enabled, Toast.LENGTH_LONG).show();
            MainActivity.getInstance().popBackstackToRoot();
        }

        if ( _originalTrigger != null ) {
            updateUI();
        }
        return view;
    }

    void updateUI() {
        if ( _originalTrigger == null ) {
            Log.e(LOG_TAG, "trigger: No _originalTrigger");
            return;
        }
        //Log.d(LOG_TAG, "trigger: _originalTrigger=[" + _originalTrigger + "]");

        if ( _originalTrigger.getPropertyNickname() == null ) {
            Log.e(LOG_TAG, "trigger: No property nickname");
        } else {
            // Select the property in our list
            String[] props = _deviceModel.getNotifiablePropertyNames();
            for ( int i = 0; i < props.length; i++ ) {
                if ( props[i].equals(_originalTrigger.getPropertyNickname()) ) {
                    _propertySpinner.setSelection(i);
                    break;
                }
            }
        }

        _nameEditText.setText(_originalTrigger.getDeviceNickname());

        switch(_originalTrigger.getBaseType()) {
            case "boolean":
                if ( _originalTrigger.getTriggerType().equals(TRIGGER_COMPARE_ABSOLUTE)) {
                    // This is either turn on or turn off
                    _booleanRadioGroup.check(_originalTrigger.getValue().equals("1") ? R.id.radio_turn_on :
                            R.id.radio_turn_off);
                } else {
                    _booleanRadioGroup.check(R.id.radio_on_or_off);
                }
                break;

            case "integer":
            case "decimal":
                _numberEditText.setText(_originalTrigger.getValue());
                if ( _originalTrigger.getTriggerType().equals(TRIGGER_COMPARE_ABSOLUTE) &&
                        _originalTrigger.getValue() != null ) {
                    _numberRadioGroup.check(_originalTrigger.getValue().equals("<") ? R.id
                            .radio_integer_less_than : R.id.radio_integer_greater_than);
                } else {
                    _numberRadioGroup.check(R.id.radio_integer_changes);
                }
                break;
        }

        // Update our list of contacts from the apps on this trigger

        _originalTrigger.fetchApps(
                new Response.Listener<AylaPropertyTriggerApp[]>() {
                    @Override
                    public void onResponse(AylaPropertyTriggerApp[] response) {
                        for (AylaPropertyTriggerApp propertyTriggerApp : response) {
                            AylaServiceApp.NotificationType notificationType= propertyTriggerApp
                                    .getNotificationType();

                            if(AylaServiceApp.NotificationType.GooglePush.equals
                                    (notificationType) || AylaServiceApp.NotificationType
                                    .BaiduPush.equals(notificationType)) {
                                //Now check if the registration id matches
                                if (PushProvider.checkDeviceMatchWithTriggerApp(propertyTriggerApp)) {
                                    _sendPushCheckbox.setChecked(true);
                                }
                            }
                            String contactId = propertyTriggerApp.getContactId();
                            if (contactId == null) {
                                continue;
                            }
                            AylaContact contact= AMAPCore.sharedInstance().getContactManager().getContactByID(Integer.parseInt
                                    (contactId));
                            if(contact == null) {
                                return;
                            }
                            switch (notificationType) {
                                case SMS:
                                    _smsContacts.add(contact);
                                    break;
                                case EMail:
                                    _emailContacts.add(contact);
                                    break;
                            }
                        }
                        if (_recyclerView != null && _recyclerView.getAdapter() != null) {
                            _recyclerView.getAdapter().notifyDataSetChanged();
                        }
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        Toast.makeText(MainActivity.getInstance(), error.toString(), Toast.LENGTH_LONG)
                                .show();
                    }
                });
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

    @Override
    public void emailTapped(AylaContact contact) {
        Log.d(LOG_TAG, "Email tapped: " + contact);
        if (TextUtils.isEmpty(contact.getEmail()) || !contact.getWantsEmailNotification()) {
            Toast.makeText(getActivity(), R.string.contact_email_required, Toast.LENGTH_SHORT).show();
            return;
        }

        if ( _emailContacts.contains(contact) ) {
            _emailContacts.remove(contact);
        } else {
            _emailContacts.add(contact);
        }
        _recyclerView.getAdapter().notifyDataSetChanged();
    }

    @Override
    public void smsTapped(AylaContact contact) {
        Log.d(LOG_TAG, "SMS tapped: " + contact);
        if (TextUtils.isEmpty(contact.getPhoneNumber()) || !contact.getWantsSmsNotification()) {
            Toast.makeText(getActivity(), R.string.contact_phone_required, Toast.LENGTH_SHORT).show();
            return;
        }

        if ( _smsContacts.contains(contact) ) {
            _smsContacts.remove(contact);
        } else {
            _smsContacts.add(contact);
        }
        _recyclerView.getAdapter().notifyDataSetChanged();
    }

    @Override
    public void contactTapped(AylaContact contact) {
        Log.d(LOG_TAG, "Contact tapped: " + contact);
        if ( _smsContacts.contains(contact) || _emailContacts.contains(contact) || _pushContacts.contains(contact) ) {
            _smsContacts.remove(contact);
            _emailContacts.remove(contact);
            _pushContacts.remove(contact);
        } else {
            AccountSettings accountSettings = AMAPCore.sharedInstance().getAccountSettings();
            if (accountSettings != null) {
                if (isOwner(contact)) {
                    _pushContacts.add(contact);
                }
            }

            if (!TextUtils.isEmpty(contact.getPhoneNumber()) && contact.getWantsSmsNotification()) {
                _smsContacts.add(contact);
            }
            if (!TextUtils.isEmpty(contact.getEmail()) && contact.getWantsEmailNotification()) {
                _emailContacts.add(contact);
            }
        }

        _recyclerView.getAdapter().notifyDataSetChanged();
    }

    @Override
    public void contactLongTapped(AylaContact contact) {
        Log.d(LOG_TAG, "Contact long tapped: " + contact);
    }

    @Override
    public int colorForIcon(AylaContact contact, IconType iconType) {
        switch ( iconType ) {
            case ICON_SMS:
                if ( _smsContacts.contains(contact) ) {
                    return MainActivity.getInstance().getResources().getColor(R.color.app_theme_accent);
                } else {
                    return MainActivity.getInstance().getResources().getColor(R.color.disabled_text);
                }

            case ICON_EMAIL:
                if ( _emailContacts.contains(contact) ) {
                    return MainActivity.getInstance().getResources().getColor(R.color.app_theme_accent);
                } else {
                    return MainActivity.getInstance().getResources().getColor(R.color.disabled_text);
                }
        }
        return MainActivity.getInstance().getResources().getColor(R.color.disabled_text);
    }

    @Override
    public void onClick(View v) {
        // Save notifications
        Log.d(LOG_TAG, "Save Notifications");

        // Make sure things are set up right
        if ( _nameEditText.getText().toString().isEmpty() ) {
            Toast.makeText(getActivity(), R.string.choose_name, Toast.LENGTH_LONG).show();
            _nameEditText.requestFocus();
            return;
        }

        String propName = _deviceModel.getNotifiablePropertyNames()[_propertySpinner
                .getSelectedItemPosition()];
        AylaProperty prop = _deviceModel.getProperty(propName);
        if ( prop == null ) {
            // Uh oh.
            Toast.makeText(getActivity(), R.string.unknown_error, Toast.LENGTH_LONG).show();
            return;
        }

        // Make sure somebody is selected
        if ( _emailContacts.size() + _smsContacts.size() + _pushContacts.size() == 0 ) {
            Toast.makeText(getActivity(), R.string.no_contacts_selected, Toast.LENGTH_LONG).show();
            return;
        }

        // If a value is required, make sure it's set
        Float numberValue;
        try{
            numberValue = Float.parseFloat(_numberEditText.getText().toString());
        } catch (NumberFormatException e) {
            Log.d(LOG_TAG, "Not a number: " + _numberEditText.getText().toString());
            numberValue = null;
        }

        if ( ("integer".equals(prop.getBaseType()) || "decimal".equals(prop.getBaseType())) &&
                (_numberRadioGroup.getCheckedRadioButtonId() != R.id.radio_integer_changes && _integerLayout.getVisibility() == View.VISIBLE) ){
            if ( numberValue == null ) {
                _numberEditText.requestFocus();
                Toast.makeText(getActivity(), R.string.no_value_chosen, Toast.LENGTH_LONG).show();
                return;
            }
        }

        // Now we should be set to create the trigger and trigger apps.
        AylaPropertyTrigger trigger = new AylaPropertyTrigger();

        trigger.setDeviceNickname(_nameEditText.getText().toString());
        trigger.setBaseType(prop.getBaseType());
        trigger.setActive(true);

        if ( prop.getBaseType().equals("boolean")) {
            switch ( _booleanRadioGroup.getCheckedRadioButtonId() ) {
                case R.id.radio_turn_on:
                    trigger.setTriggerType(TRIGGER_COMPARE_ABSOLUTE);
                    trigger.setCompareType("==");
                    trigger.setValue("1");
                    break;

                case R.id.radio_turn_off:
                    trigger.setTriggerType(TRIGGER_COMPARE_ABSOLUTE);
                    trigger.setCompareType("==");
                    trigger.setValue("0");
                    break;

                case R.id.radio_on_or_off:
                    trigger.setTriggerType(TRIGGER_ALWAYS);
                    break;
            }
        } else if(prop.getBaseType().equals("integer")){
            if(_deviceModel.friendlyNameForPropertyName(propName).equals(MainActivity.getInstance()
                    .getString(R.string
                    .property_motion_sensor_friendly_name))) {
                switch (_motionRadioGroup.getCheckedRadioButtonId()){
                    case R.id.radio_detected:
                        trigger.setTriggerType(TRIGGER_ALWAYS);
                        break;
                    case R.id.radio_stopped:
                        trigger.setTriggerType(TRIGGER_ALWAYS);
                        break;
                }
            } else{
                switch ( _numberRadioGroup.getCheckedRadioButtonId() ) {
                    case R.id.radio_integer_changes:
                        trigger.setTriggerType(TRIGGER_ALWAYS);
                        break;

                    case R.id.radio_integer_greater_than:
                        trigger.setTriggerType(TRIGGER_COMPARE_ABSOLUTE);
                        trigger.setCompareType(">");
                        break;

                    case R.id.radio_integer_less_than:
                        trigger.setTriggerType(TRIGGER_COMPARE_ABSOLUTE);
                        trigger.setCompareType("<");
                        break;
                }
            }
        }

        MainActivity.getInstance().showWaitDialog(R.string.updating_notifications_title, R.string.updating_notifications_body);

        _propertyNotificationHelper.setNotifications(prop,
                trigger,
                _pushContacts,
                _emailContacts,
                _smsContacts,
                new PropertyNotificationHelper.SetNotificationListener() {
                    public void notificationsSet(AylaProperty property, AylaPropertyTrigger
                            propertyTrigger, AylaError error) {
                        MainActivity.getInstance().dismissWaitDialog();

                        if (_originalTrigger != null) {
                            property.deleteTrigger(_originalTrigger, new Response.Listener<
                                            AylaAPIRequest.EmptyResponse>() {
                                        @Override
                                        public void onResponse(AylaAPIRequest.EmptyResponse response) {
                                            AylaLog.d(LOG_TAG, "Successfully Deleted the old trigger");
                                            getFragmentManager().popBackStack();
                                        }
                                    },
                                    new ErrorListener() {
                                        @Override
                                        public void onErrorResponse(AylaError error) {
                                            Toast.makeText(MainActivity.getInstance(), error.toString(), Toast.LENGTH_LONG)
                                                    .show();
                                        }
                                    });
                        }
                        else {
                            getFragmentManager().popBackStack();
                        }
                    }
                });
    }
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String propertyName = _deviceModel.getNotifiablePropertyNames()[position];
        Log.d(LOG_TAG, "onItemSelected: "+ _propertySpinner.getSelectedItem() + " == " + propertyName);
        AylaProperty prop = _deviceModel.getProperty(propertyName);
        if ( prop == null ) {
            Log.e(LOG_TAG, "Failed to get property: " + propertyName);
            return;
        }

        // Hide all of the type-specific layouts
        _booleanLayout.setVisibility(View.INVISIBLE);
        _integerLayout.setVisibility(View.INVISIBLE);
        _motionSensorLayout.setVisibility(View.INVISIBLE);
        Log.d(LOG_TAG, "Property " + propertyName + " base type: " + prop.getBaseType());
        _numberRadioGroup.clearCheck();
        switch ( prop.getBaseType() ) {
            case "boolean":
                _booleanLayout.setVisibility(View.VISIBLE);
                break;

            case "string":
                // TODO: Do we even want to support string properties? Not sure what the UI would be...
                Log.e(LOG_TAG, "String: Not yet implemented");
                break;

            case "integer":

                if(_deviceModel.friendlyNameForPropertyName(propertyName).equals(MainActivity
                        .getInstance().getString(R.string.property_motion_sensor_friendly_name))){
                    _motionSensorLayout.setVisibility(View.VISIBLE);

                } else{
                    _integerLayout.setVisibility(View.VISIBLE);
                    _numberEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
                    _numberRadioGroup.check(R.id.radio_integer_changes);

                }
                break;


            case "decimal":
                _integerLayout.setVisibility(View.VISIBLE);
                _numberEditText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                _numberRadioGroup.check(R.id.radio_integer_changes);
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        Log.e(LOG_TAG, "Nothing selected!");
    }
}
