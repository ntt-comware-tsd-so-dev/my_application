package com.aylanetworks.agilelink.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.aylanetworks.aaml.AylaApplicationTrigger;
import com.aylanetworks.aaml.AylaContact;
import com.aylanetworks.aaml.AylaProperty;
import com.aylanetworks.aaml.AylaPropertyTrigger;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.fragments.adapters.ContactListAdapter;
import com.aylanetworks.agilelink.framework.ContactManager;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.PropertyNotificationHelper;
import com.aylanetworks.agilelink.framework.SessionManager;

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
    private List<AylaContact> _emailContacts;
    private List<AylaContact> _smsContacts;
    private Spinner _propertySpinner;
    private EditText _nameEditText;
    private EditText _numberEditText;
    private RadioGroup _numberRadioGroup;
    private RadioGroup _booleanRadioGroup;
    private String _originalTriggerName;
    private AylaPropertyTrigger _originalTrigger;
    private PropertyNotificationHelper _propertyNotificationHelper;

    // Layouts for each of the property base types. We will enable the appropriate layout
    // when the property is selected.
    private LinearLayout _booleanLayout;
    private LinearLayout _integerLayout;

    public static PropertyNotificationFragment newInstance(Device device) {
        return newInstance(device, null);
    }

    public static PropertyNotificationFragment newInstance(Device device, AylaPropertyTrigger triggerToEdit) {
        PropertyNotificationFragment frag = new PropertyNotificationFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DSN, device.getDevice().dsn);
        if ( triggerToEdit != null ) {
            args.putString(ARG_TRIGGER, triggerToEdit.deviceNickname);
        }
        frag.setArguments(args);

        return frag;
    }

    // Default constructor
    public PropertyNotificationFragment() {
        _emailContacts = new ArrayList<>();
        _smsContacts = new ArrayList<>();
    }

    private Device _device;

    private RecyclerView _recyclerView;
    private RecyclerView.LayoutManager _layoutManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        _device = null;
        if (getArguments() != null ) {
            String dsn = getArguments().getString(ARG_DSN);
            _device = SessionManager.deviceManager().deviceByDSN(dsn);
            Log.d(LOG_TAG, "My device: " + _device);

            _propertyNotificationHelper = new PropertyNotificationHelper(_device);

            _originalTriggerName = getArguments().getString(ARG_TRIGGER);
            if ( _originalTriggerName != null ) {
                // Try to find the trigger
                for ( AylaProperty prop : _device.getDevice().properties ) {
                    for ( AylaPropertyTrigger trigger : prop.propertyTriggers ) {
                        if ( _originalTriggerName.equals(trigger.deviceNickname) ) {
                            _originalTrigger = trigger;
                            break;
                        }
                    }
                }

                if ( _originalTrigger == null ) {
                    Log.e(LOG_TAG, "Unable to find original trigger!");
                }
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_property_notification, container, false);

        _recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        _layoutManager = new LinearLayoutManager(getActivity());
        _recyclerView.setLayoutManager(_layoutManager);
        _nameEditText = (EditText)view.findViewById(R.id.notification_name);
        _propertySpinner = (Spinner)view.findViewById(R.id.property_spinner);
        _booleanLayout = (LinearLayout)view.findViewById(R.id.layout_boolean);
        _integerLayout = (LinearLayout)view.findViewById(R.id.layout_integer);
        _numberEditText = (EditText)view.findViewById(R.id.number_edit_text);

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

        String propertyNames[] = _device.getNotifiablePropertyNames();
        String friendlyNames[] = new String[propertyNames.length];
        for ( int i = 0; i < propertyNames.length; i++ ) {
            friendlyNames[i] = _device.friendlyNameForPropertyName(propertyNames[i]);
        }

        _propertySpinner.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, friendlyNames));
        _propertySpinner.setOnItemSelectedListener(this);
        view.findViewById(R.id.save_notifications).setOnClickListener(this);

        final TextView emptyView = (TextView)view.findViewById(R.id.empty);

        // Fetch the list of contacts, including the owner contact, and show them in the list
        ContactManager cm = SessionManager.getInstance().getContactManager();
        ContactManager.ContactManagerListener listener = new ContactManager.ContactManagerListener() {
            @Override
            public void contactListUpdated(ContactManager manager, boolean succeeded) {
                _recyclerView.setAdapter(new ContactListAdapter(true, PropertyNotificationFragment.this));
                emptyView.setVisibility(View.INVISIBLE);
                _recyclerView.setVisibility(View.VISIBLE);
            }
        };

        if ( cm.getContacts(true).isEmpty() ) {
            cm.fetchContacts(listener, true);
        } else {
            listener.contactListUpdated(cm, true);
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
            return;
        }

        if ( _originalTrigger.propertyNickname == null ) {
            Log.e(LOG_TAG, "No property nickname");
        } else {
            // Select the property in our list
            String[] props = _device.getNotifiablePropertyNames();
            for ( int i = 0; i < props.length; i++ ) {
                if ( props[i].equals(_originalTrigger.propertyNickname) ) {
                    _propertySpinner.setSelection(i);
                    break;
                }
            }
        }

        _nameEditText.setText(_originalTrigger.deviceNickname);

        switch(_originalTrigger.baseType) {
            case "boolean":
                if ( _originalTrigger.triggerType.equals(TRIGGER_COMPARE_ABSOLUTE)) {
                    // This is either turn on or turn off
                    _booleanRadioGroup.check(_originalTrigger.value.equals("1") ? R.id.radio_turn_on : R.id.radio_turn_off);
                } else {
                    _booleanRadioGroup.check(R.id.radio_on_or_off);
                }
                break;

            case "integer":
            case "decimal":
                _numberEditText.setText(_originalTrigger.value);
                if ( _originalTrigger.triggerType.equals(TRIGGER_COMPARE_ABSOLUTE) && _originalTrigger.value != null ) {
                    _numberRadioGroup.check(_originalTrigger.value.equals("<") ? R.id.radio_integer_less_than : R.id.radio_integer_greater_than);
                } else {
                    _numberRadioGroup.check(R.id.radio_integer_changes);
                }
                break;
        }

        // Update our list of contacts from the apps on this trigger
        if ( _originalTrigger.applicationTriggers == null ) {
            Log.e(LOG_TAG, "No triggers found for this notification");
        } else {
            for ( AylaApplicationTrigger trigger : _originalTrigger.applicationTriggers ) {
                AylaContact contact = SessionManager.getInstance().getContactManager().getContactByID(Integer.parseInt(trigger.contact_id));
                if ( contact != null ) {
                    if ( trigger.name.equals("email") ) {
                        _emailContacts.add(contact);
                    } else {
                        _smsContacts.add(contact);
                    }
                }
            }
        }

        _recyclerView.getAdapter().notifyDataSetChanged();
    }

    @Override
    public void emailTapped(AylaContact contact) {
        Log.d(LOG_TAG, "Email tapped: " + contact);
        if (TextUtils.isEmpty(contact.email)) {
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
        if (TextUtils.isEmpty(contact.phoneNumber)) {
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
        if ( _smsContacts.contains(contact) || _emailContacts.contains(contact) ) {
            _smsContacts.remove(contact);
            _emailContacts.remove(contact);
        } else {
            if ( !TextUtils.isEmpty(contact.phoneNumber) ) {
                _smsContacts.add(contact);
            }
            if ( !TextUtils.isEmpty(contact.email) ) {
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

        String propName = _device.getNotifiablePropertyNames()[_propertySpinner.getSelectedItemPosition()];
        AylaProperty prop = _device.getProperty(propName);
        if ( prop == null ) {
            // Uh oh.
            Toast.makeText(getActivity(), R.string.unknown_error, Toast.LENGTH_LONG).show();
            return;
        }

        // Make sure somebody is selected
        if ( _emailContacts.size() + _smsContacts.size() == 0 ) {
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

        if ( ("integer".equals(prop.baseType) || "decimal".equals(prop.baseType)) &&
                _numberRadioGroup.getCheckedRadioButtonId() != R.id.radio_integer_changes) {
            if ( numberValue == null ) {
                _numberEditText.requestFocus();
                Toast.makeText(getActivity(), R.string.no_value_chosen, Toast.LENGTH_LONG).show();
                return;
            }
        }

        // Now we should be set to create the trigger and trigger apps.
        AylaPropertyTrigger trigger = new AylaPropertyTrigger();

        trigger.deviceNickname = _nameEditText.getText().toString();
        trigger.baseType = prop.baseType;
        trigger.active = true;

        if ( prop.baseType.equals("boolean")) {
            switch ( _booleanRadioGroup.getCheckedRadioButtonId() ) {
                case R.id.radio_turn_on:
                    trigger.triggerType = TRIGGER_COMPARE_ABSOLUTE;
                    trigger.compareType = "==";
                    trigger.value = "1";
                    break;

                case R.id.radio_turn_off:
                    trigger.triggerType = TRIGGER_COMPARE_ABSOLUTE;
                    trigger.compareType = "==";
                    trigger.value = "0";
                    break;

                case R.id.radio_on_or_off:
                    trigger.triggerType = TRIGGER_ALWAYS;
                    break;
            }
        } else {
            switch ( _numberRadioGroup.getCheckedRadioButtonId() ) {
                case R.id.radio_integer_changes:
                    trigger.triggerType = TRIGGER_ALWAYS;
                    break;

                case R.id.radio_integer_greater_than:
                    trigger.triggerType = TRIGGER_COMPARE_ABSOLUTE;
                    trigger.compareType = ">";
                    break;

                case R.id.radio_integer_less_than:
                    trigger.triggerType = TRIGGER_COMPARE_ABSOLUTE;
                    trigger.compareType = "<";
                    break;
            }
        }

        MainActivity.getInstance().showWaitDialog(R.string.updating_notifications_title, R.string.updating_notifications_body);

        _propertyNotificationHelper.setNotifications(prop,
                trigger,
                _emailContacts,
                _smsContacts,
                new PropertyNotificationHelper.SetNotificationListener() {
            @Override
            public void notificationsSet(AylaProperty property, AylaPropertyTrigger propertyTrigger, boolean succeeded) {
                MainActivity.getInstance().dismissWaitDialog();
                if ( succeeded ) {
                    Toast.makeText(getActivity(), R.string.notification_created, Toast.LENGTH_LONG).show();

                    // Delete the old notification. We won't wait for the response.
                    if ( _originalTrigger != null ) {
                        _originalTrigger.destroyTrigger(new Handler() {
                            @Override
                            public void handleMessage(Message msg) {
                                Log.d(LOG_TAG, "Old trigger deletion: " + msg);
                            }
                        });
                    }
                    getFragmentManager().popBackStack();
                } else {
                    Log.e(LOG_TAG, "Failed to set notifications");
                    String msg = (String)_lastMessage.obj;
                    if ( TextUtils.isEmpty(msg) ) {
                        msg = getActivity().getString(R.string.unknown_error);
                    }
                    Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String propertyName = _device.getNotifiablePropertyNames()[position];
        Log.d(LOG_TAG, "onItemSelected: "+ _propertySpinner.getSelectedItem() + " == " + propertyName);
        AylaProperty prop = _device.getProperty(propertyName);
        if ( prop == null ) {
            Log.e(LOG_TAG, "Failed to get property: " + propertyName);
            return;
        }

        // Hide all of the type-specific layouts
        _booleanLayout.setVisibility(View.INVISIBLE);
        _integerLayout.setVisibility(View.INVISIBLE);
        Log.d(LOG_TAG, "Property " + propertyName + " base type: " + prop.baseType);
        _numberRadioGroup.clearCheck();
        switch ( prop.baseType ) {
            case "boolean":
                _booleanLayout.setVisibility(View.VISIBLE);
                break;

            case "string":
                // TODO: Do we even want to support string properties? Not sure what the UI would be...
                Log.e(LOG_TAG, "String: Not yet implemented");
                break;

            case "integer":

                _integerLayout.setVisibility(View.VISIBLE);
                _numberEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
                _numberRadioGroup.check(R.id.radio_integer_changes);
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
