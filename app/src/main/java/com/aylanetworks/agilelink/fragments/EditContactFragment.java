package com.aylanetworks.agilelink.fragments;

import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.aylanetworks.agilelink.ErrorUtils;
import com.aylanetworks.agilelink.framework.AMAPCore;
import com.aylanetworks.aylasdk.AylaContact;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.controls.ComboBox;
import com.aylanetworks.agilelink.framework.ContactManager;
import com.aylanetworks.agilelink.framework.Logger;
import com.aylanetworks.aylasdk.error.AylaError;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.util.ArrayList;
import java.util.List;

import static android.provider.ContactsContract.CommonDataKinds.Email;
import static android.provider.ContactsContract.CommonDataKinds.Phone;
import static android.provider.ContactsContract.CommonDataKinds.StructuredName;
import static android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import static android.provider.ContactsContract.Data;

/**
 * Created by Brian King on 3/19/15.
 */
public class EditContactFragment extends Fragment implements View.OnClickListener, TextWatcher, MainActivity.PickContactListener {

    private EditText _firstName;
    private EditText _lastName;
    private EditText _displayName;
    private ComboBox _email;
    private EditText _countryCode;
    private ComboBox _phoneNumber;
    private EditText _streetAddress;
    private EditText _zipCode;
    private Button _button;
    private Button _buttonDelete;

    private final static String LOG_TAG = "EditContactFragment";
    private final static String ARG_CONTACT_ID = "contact_id";

    private AylaContact _aylaContact;

    private boolean _dontDismiss;       // Set to true if we pick a contact from here rather than
                                        // from the contact list fragment

    public static EditContactFragment newInstance(AylaContact contact) {
        Bundle args = new Bundle();
        args.putInt(ARG_CONTACT_ID, contact == null ? 0 : contact.getId());
        EditContactFragment frag = new EditContactFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        getActivity().invalidateOptionsMenu();
        int contactID = getArguments().getInt(ARG_CONTACT_ID);
        if (contactID != 0) {
            _aylaContact = AMAPCore.sharedInstance().getContactManager().getContactByID(contactID);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_fill_contact, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch ( item.getItemId() ) {
            case R.id.action_fill_from_contact2:
                _dontDismiss = true;
                MainActivity.getInstance().pickContact(this);
                return true;
        }

        return false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_contact, container, false);

        _firstName = (EditText) view.findViewById(R.id.first_name);
        _lastName = (EditText) view.findViewById(R.id.last_name);
        _displayName = (EditText) view.findViewById(R.id.display_name);
        _email = (ComboBox) view.findViewById(R.id.email_address);
        _countryCode = (EditText) view.findViewById(R.id.country_code);
        _phoneNumber = (ComboBox) view.findViewById(R.id.phone_number);
        _streetAddress = (EditText) view.findViewById(R.id.street_address);
        _zipCode = (EditText) view.findViewById(R.id.zip_code);
        _button = (Button) view.findViewById(R.id.save_contact);
        _buttonDelete = (Button) view.findViewById(R.id.delete_contact);

        _firstName.addTextChangedListener(this);
        _lastName.addTextChangedListener(this);

        _button.setOnClickListener(this);
        _buttonDelete.setOnClickListener(this);
        _buttonDelete.setVisibility((_aylaContact == null)? View.GONE : View.VISIBLE);

        setupViews();

        return view;
    }

    private void setupViews() {
        if (_aylaContact == null) {
            _firstName.setText("");
            _lastName.setText("");
            _displayName.setText("");
            _email.setText("");
            _countryCode.setText("");
            _phoneNumber.setText("");
            _streetAddress.setText("");
            _zipCode.setText("");
            _button.setText(getString(R.string.create_contact));
        } else {
            _firstName.setText(_aylaContact.getFirstname());
            _lastName.setText(_aylaContact.getLastname());
            _displayName.setText(_aylaContact.getDisplayName());
            _email.setText(_aylaContact.getEmail());
            _countryCode.setText(_aylaContact.getPhoneCountryCode());

          /*  PhoneNumberUtil util = PhoneNumberUtil.getInstance();
            Phonenumber.PhoneNumber phoneNumber = null;
            try {
                phoneNumber = util.parse(_aylaContact.phoneNumber, "US");
            } catch (NumberParseException e) {
                e.printStackTrace();
            }

            if ( phoneNumber != null ) {
                _phoneNumber.setText(util.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164));
            } else {
                _phoneNumber.setText(_aylaContact.phoneNumber);
            }*/

            _phoneNumber.setText(_aylaContact.getPhoneNumber());
            _streetAddress.setText(_aylaContact.getStreetAddress());
            _zipCode.setText(_aylaContact.getZipCode());
            _button.setText(getString(R.string.update_contact));
        }
    }

    private boolean validateFields() {
        if (_phoneNumber.getText().length() == 0 && _email.getText().length() == 0) {
            Toast.makeText(getActivity(), R.string.contact_email_phone_required, Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    void onSaveContactClicked() {
        if (!validateFields()) {
            return;
        }

        // Save the contact
        if (_aylaContact == null) {
            _aylaContact = new AylaContact();
        }

        _aylaContact.setFirstname(_firstName.getText().toString());
        _aylaContact.setLastname(_lastName.getText().toString());
        _aylaContact.setDisplayName(_displayName.getText().toString());
        _aylaContact.setEmail(_email.getText());
        _aylaContact.setPhoneCountryCode(_countryCode.getText().toString());
        _aylaContact.setPhoneNumber(_phoneNumber.getText());

        // The server is picky about the format of the phone number
        _aylaContact.setPhoneNumber(_aylaContact.getPhoneNumber().replaceAll("[^0-9]", ""));

        _aylaContact.setStreetAddress(_streetAddress.getText().toString());
        _aylaContact.setZipCode(_zipCode.getText().toString());
        _aylaContact.setWantsEmailNotification(true);
        _aylaContact.setWantsSmsNotification(true);

        final ContactManager.ContactManagerListener listener = new ContactManager.ContactManagerListener() {
            @Override
            public void contactListUpdated(ContactManager manager, AylaError error) {
                MainActivity.getInstance().dismissWaitDialog();
                if (error == null) {
                    Toast.makeText(getActivity(), R.string.contact_updated, Toast.LENGTH_LONG).show();
                    getFragmentManager().popBackStack();
                } else {
                    Toast.makeText(getActivity(),
                            ErrorUtils.getUserMessage(getActivity(), error, R.string.contact_update_failed),
                            Toast.LENGTH_SHORT).show();
                }
            }
        };

        MainActivity.getInstance().showWaitDialog(R.string.updating_contact_title, R.string.updating_contact_body);
        // Do we add or update?
        if (_aylaContact.getId() == null) {
            // Add.
            AMAPCore.sharedInstance().getContactManager().addContact(_aylaContact, listener);
        } else {
            AMAPCore.sharedInstance().getContactManager().updateContact(_aylaContact, listener);
        }
    }

    void onDeleteContactClicked() {
        MainActivity.getInstance().showWaitDialog(R.string.deleting_contact_title, R.string.deleting_contact_body);
        AMAPCore.sharedInstance().getContactManager().deleteContact(_aylaContact, new ContactManager.ContactManagerListener() {
            @Override
            public void contactListUpdated(ContactManager manager, AylaError error) {
                MainActivity.getInstance().dismissWaitDialog();
                if ( error == null ) {
                    Toast.makeText(getActivity(), R.string.contact_deleted, Toast.LENGTH_LONG).show();
                    getFragmentManager().popBackStack();
                } else {
                    Toast.makeText(MainActivity.getInstance(),
                            ErrorUtils.getUserMessage(getActivity(), error, R.string.unknown_error),
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.save_contact:
                onSaveContactClicked();
                break;

            case R.id.delete_contact:
                onDeleteContactClicked();
                break;
        }
    }

    // Text change listener methods

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        StringBuilder sb = new StringBuilder();
        String first = _firstName.getText().toString();
        String last = _lastName.getText().toString();

        if (first.length() > 0) {
            sb.append(first);
            if (last.length() > 0) {
                sb.append(" ");
            }
        }
        if (last.length() > 0) {
            sb.append(last);
        }

        _displayName.setText(sb.toString());
    }

    @Override
    public void contactPicked(Cursor cursor) {
        Log.d(LOG_TAG, "Contact picked: " + cursor);
        if (cursor == null) {
            // Cancel. Pop ourselves off the stack if we were launched from elsewhere
            if ( !_dontDismiss) {
                try {
                    // have to do this to handle cancel from onActivityResult
                    Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            getFragmentManager().popBackStack();
                        }
                    };
                    Handler h = new Handler();
                    h.post(r);
                } catch (Exception ex) {
                    Logger.logError(LOG_TAG, ex);
                }
            }
            return;
        }

        // Set up some arraylists for email addresses and phone numbers
        List<String> phoneNumbers = new ArrayList<>();
        List<String> emailAddresses = new ArrayList<>();

        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

        while (cursor.moveToNext()) {
            int mimeTypeIndex = cursor.getColumnIndex(Data.MIMETYPE);
            if (mimeTypeIndex == -1) {
                Log.e(LOG_TAG, "MIME type not found for contact");
                continue;
            }

            String mimeType = cursor.getString(mimeTypeIndex);

            switch (mimeType) {
                case StructuredName.CONTENT_ITEM_TYPE:
                    int indexGiven = cursor.getColumnIndex(StructuredName.GIVEN_NAME);
                    int indexFamily = cursor.getColumnIndex(StructuredName.FAMILY_NAME);
                    if (indexGiven != -1) {
                        String firstName = cursor.getString(indexGiven);
                        if (firstName != null) {
                            _firstName.setText(firstName);
                        }
                    }
                    if (indexFamily != -1) {
                        String lastName = cursor.getString(indexFamily);
                        if (lastName != null) {
                            _lastName.setText(lastName);
                        }
                    }
                    break;

                case Email.CONTENT_ITEM_TYPE:
                    int indexMail = cursor.getColumnIndex(Email.DATA);
                    if (indexMail != -1) {
                        String addr = cursor.getString(indexMail);
                        emailAddresses.add(addr);
                    }
                    break;

                case Phone.CONTENT_ITEM_TYPE:
                    int indexPhone = cursor.getColumnIndex(Phone.NUMBER);
                    if (indexPhone != -1) {
                        String phone = cursor.getString(indexPhone);
                        phoneNumbers.add(phone);
                    }
                    break;

                case StructuredPostal.CONTENT_ITEM_TYPE:
                    int indexAddress = cursor.getColumnIndex(StructuredPostal.FORMATTED_ADDRESS);
                    if (indexAddress != -1) {
                        String address = cursor.getString(indexAddress);
                        _streetAddress.setText(address);
                    }

                    int indexZip = cursor.getColumnIndex(StructuredPostal.POSTCODE);
                    if (indexZip != -1) {
                        _zipCode.setText(cursor.getString(indexZip));
                    }
                    break;

                default:
                    Log.e(LOG_TAG, "Unknown MIME type: " + mimeType);
                    break;
            }
        }

        // Set our adapters
        String[] emails = emailAddresses.toArray(new String[emailAddresses.size()]);
        ArrayAdapter emailAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, emails);
        _email.setAdapter(emailAdapter);

        String[] phones = phoneNumbers.toArray(new String[phoneNumbers.size()]);
        ArrayAdapter phoneAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, phones);
        _phoneNumber.setAdapter(phoneAdapter);

        if (phones.length > 0) {
            try {
                Phonenumber.PhoneNumber num = phoneUtil.parse(phones[0], "US");
                Integer countryCode = num.getCountryCode();
                if (countryCode != 0) {
                    _countryCode.setText(countryCode.toString());
                } else {
                    _countryCode.setText("1");
                }
            } catch (NumberParseException e) {
                e.printStackTrace();
            }
        }
    }
}
