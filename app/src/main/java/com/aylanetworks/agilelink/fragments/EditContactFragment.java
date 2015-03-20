package com.aylanetworks.agilelink.fragments;

import android.database.Cursor;
import android.os.Bundle;
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
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.aylanetworks.aaml.AylaContact;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.controls.ComboBox;
import com.aylanetworks.agilelink.framework.ContactManager;
import com.aylanetworks.agilelink.framework.SessionManager;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.util.ArrayList;
import java.util.List;

import static android.provider.ContactsContract.*;
import static android.provider.ContactsContract.CommonDataKinds.*;

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

    private final static String LOG_TAG = "EditContactFragment";
    private final static String ARG_CONTACT_ID = "contact_id";

    private AylaContact _aylaContact;

    public static EditContactFragment newInstance(AylaContact contact) {
        Bundle args = new Bundle();
        args.putInt(ARG_CONTACT_ID, contact == null ? 0 : contact.id);
        EditContactFragment frag = new EditContactFragment();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        int contactID = getArguments().getInt(ARG_CONTACT_ID);
        if (contactID != 0) {
            _aylaContact = SessionManager.getInstance().getContactManager().getContactByID(contactID);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_add_contact, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Add from contact
        MainActivity.getInstance().pickContact(this);
        return true;
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

        _firstName.addTextChangedListener(this);
        _lastName.addTextChangedListener(this);

        _button.setOnClickListener(this);

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
            _firstName.setText(_aylaContact.firstname);
            _lastName.setText(_aylaContact.lastname);
            _displayName.setText(_aylaContact.displayName);
            _email.setText(_aylaContact.email);
            _countryCode.setText(_aylaContact.phoneCountryCode);
            _phoneNumber.setText(_aylaContact.phoneNumber);
            _streetAddress.setText(_aylaContact.streetAddress);
            _zipCode.setText(_aylaContact.zipCode);
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

    @Override
    public void onClick(View v) {
        if (!validateFields()) {
            return;
        }

        // Save the contact
        if (_aylaContact == null) {
            _aylaContact = new AylaContact();
        }

        _aylaContact.firstname = _firstName.getText().toString();
        _aylaContact.lastname = _lastName.getText().toString();
        _aylaContact.displayName = _displayName.getText().toString();
        _aylaContact.email = _email.getText();
        _aylaContact.phoneCountryCode = _countryCode.getText().toString();
        _aylaContact.phoneNumber = _phoneNumber.getText().toString();
        _aylaContact.streetAddress = _streetAddress.getText().toString();
        _aylaContact.zipCode = _zipCode.getText().toString();

        final ContactManager.ContactManagerListener listener = new ContactManager.ContactManagerListener() {
            @Override
            public void contactListUpdated(ContactManager manager, boolean succeeded) {
                MainActivity.getInstance().dismissWaitDialog();
                if (succeeded) {
                    Toast.makeText(getActivity(), R.string.contact_updated, Toast.LENGTH_LONG).show();
                    getFragmentManager().popBackStack();
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

        MainActivity.getInstance().showWaitDialog(R.string.updating_contact_title, R.string.updating_contact_body);
        // Do we add or update?
        if (_aylaContact.id == null) {
            // Add.
            SessionManager.getInstance().getContactManager().addContact(_aylaContact, listener);
        } else {
            SessionManager.getInstance().getContactManager().updateContact(_aylaContact, listener);
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
                    if ( indexZip != -1 ) {
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

        if ( phones.length > 0 ) {
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
