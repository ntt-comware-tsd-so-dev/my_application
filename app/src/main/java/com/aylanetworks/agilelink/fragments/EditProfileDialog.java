package com.aylanetworks.agilelink.fragments;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.aylanetworks.aaml.AylaContact;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.aaml.AylaUser;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.ContactManager;
import com.aylanetworks.agilelink.framework.SessionManager;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/*
 * EditProfileDialog.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 2/4.15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class EditProfileDialog extends Dialog implements View.OnClickListener {
    private final static String LOG_TAG = "EditProfileDialog";

    private EditText _email;
    private EditText _password;
    private EditText _confirmPassword;
    private EditText _firstName;
    private EditText _lastName;
    private EditText _country;
    private EditText _zip;
    private EditText _phoneNumber;
    private EditText _evbNumber;

    public EditProfileDialog(final Context context) {
        super(context, R.style.FullHeightDialog);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.sign_up);

        // Make the dialog full-screen
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.MATCH_PARENT;
        getWindow().setAttributes(params);

        // Set up a handler for our button
        Button b = (Button) findViewById(R.id.btnSignUp);
        b.setOnClickListener(this);

        _email = (EditText) getWindow().findViewById(R.id.etEmail);
        _password = (EditText) getWindow().findViewById(R.id.etPassword);
        _confirmPassword = (EditText) getWindow().findViewById(R.id.etConfirmPassword);
        _firstName = (EditText) getWindow().findViewById(R.id.etFirstName);
        _lastName = (EditText) getWindow().findViewById(R.id.etLastName);
        _country = (EditText) getWindow().findViewById(R.id.etCountry);
        _zip = (EditText) getWindow().findViewById(R.id.etZipCode);
        _phoneNumber = (EditText) getWindow().findViewById(R.id.etPhoneNumber);
        _evbNumber = (EditText) getWindow().findViewById(R.id.etEvbNumber);

        // Make some fields read-only
        _email.setEnabled(false);
        _evbNumber.setEnabled(false);

        // Change the name of the button to "Update Profile"
        b.setText(R.string.update_profile);

        // Update our current user's information
        Map<String, String> args = new HashMap<>();
        args.put("access_token", SessionManager.sessionParameters().accessToken);
        AylaUser.getInfo(_getInfoHandler, args);
        String title = MainActivity.getInstance().getResources().getString(R.string.fetching_user_info_title);
        String body = MainActivity.getInstance().getResources().getString(R.string.fetching_user_info_body);
        MainActivity.getInstance().showWaitDialog(title, body);
    }

    private void updateFields() {
        AylaUser currentUser = AylaUser.getCurrent();

        _email.setText(currentUser.email);
        _password.setText("");
        _confirmPassword.setText("");
        _firstName.setText(currentUser.firstname);
        _lastName.setText(currentUser.lastname);
        _country.setText(currentUser.country);
        _zip.setText(currentUser.zip);
        _phoneNumber.setText(currentUser.phone);
        _evbNumber.setText(currentUser.aylaDevKitNum);
    }

    private Map<String, String> getParameters() {
        Map<String, String> params = new HashMap<>();
        params.put("firstname", _firstName.getText().toString());
        params.put("lastname", _lastName.getText().toString());
        params.put("country", _country.getText().toString());
        params.put("zip", _zip.getText().toString());
        params.put("phone", _phoneNumber.getText().toString());
        return params;
    }

    @Override
    public void onClick(View v) {
        Log.d(LOG_TAG, "Update Profile clicked");

        String title = MainActivity.getInstance().getResources().getString(R.string.updating_profile_title);
        String body = MainActivity.getInstance().getResources().getString(R.string.updating_profile_body);

        // First check to see if the password has been updated. If so, we'll do that first.
        if (_password.getText().length() > 0) {
            String newPassword = _password.getText().toString();
            String confirm = _confirmPassword.getText().toString();
            if (!newPassword.equals(confirm)) {
                _password.setText("");
                _confirmPassword.setText("");
                _password.requestFocus();
                Toast.makeText(MainActivity.getInstance(), R.string.password_no_match, Toast.LENGTH_SHORT).show();
            } else {
                // Update the password
                MainActivity.getInstance().showWaitDialog(title, body);
                String currentPassword = SessionManager.sessionParameters().password;
                AylaUser.changePassword(_changePasswordHandler, currentPassword, newPassword);
            }
        } else {
            // Normal profile update
            MainActivity.getInstance().showWaitDialog(title, body);
            SessionManager.SessionParameters params = SessionManager.sessionParameters();
            AylaUser.updateInfo(_updateProfileHandler, getParameters(), params.appId, params.appSecret);
        }
    }

    static class ChangePasswordHandler extends Handler {
        private WeakReference<EditProfileDialog> _editProfileDialog;
        public ChangePasswordHandler(EditProfileDialog editProfileDialog) {
            _editProfileDialog = new WeakReference<EditProfileDialog>(editProfileDialog);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(LOG_TAG, "Change password handler: " + msg);
            String jsonResults = (String) msg.obj;

            if (msg.what == AylaNetworks.AML_ERROR_OK) {
                SessionManager.SessionParameters params = SessionManager.sessionParameters();
                params.password = _editProfileDialog.get()._password.getText().toString();

                // Clear out the password edit fields so we don't try to set the password again
                _editProfileDialog.get()._password.setText("");
                _editProfileDialog.get()._confirmPassword.setText("");

                // Continue updating the rest of the information
                _editProfileDialog.get().onClick(_editProfileDialog.get().getWindow().findViewById(R.id.btnSignUp));
            } else {
                MainActivity.getInstance().dismissWaitDialog();
                String errMsg = null;

                if (msg.arg1 == AylaNetworks.AML_USER_INVALID_PARAMETERS) {
                    AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "E", "amca.signin", "errors", jsonResults, "myProfile");

                    AylaUser aylaUser = AylaSystemUtils.gson.fromJson(jsonResults, AylaUser.class);
                    if (aylaUser.password != null) {
                        _editProfileDialog.get()._password.requestFocus();
                        errMsg = _editProfileDialog.get()._password.getHint() + " " + aylaUser.password;
                        Toast.makeText(MainActivity.getInstance(), errMsg, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(MainActivity.getInstance(), R.string.error_changing_profile, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private ChangePasswordHandler _changePasswordHandler = new ChangePasswordHandler(this);

    static class GetInfoHandler extends Handler {
        private WeakReference<EditProfileDialog> _editProfileDialog;

        public GetInfoHandler(EditProfileDialog editProfileDialog) {
            _editProfileDialog = new WeakReference<EditProfileDialog>(editProfileDialog);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(LOG_TAG, "_getInfoHandler: " + msg);
            MainActivity.getInstance().dismissWaitDialog();
            if (msg.what == AylaNetworks.AML_ERROR_OK) {
                String json = (String) msg.obj;
                AylaUser user = AylaSystemUtils.gson.fromJson(json, AylaUser.class);
                Log.d(LOG_TAG, "User: " + user);

                // Save the auth info- it's not filled out in the returned user object
                AylaUser oldUser = AylaUser.getCurrent();
                user.setAccessToken(oldUser.getAccessToken());
                user.setRefreshToken(oldUser.getRefreshToken());
                user.setExpiresIn(oldUser.getExpiresIn());

                AylaUser.setCurrent(user);
                _editProfileDialog.get().updateFields();
            }
        }
    }

    private GetInfoHandler _getInfoHandler = new GetInfoHandler(this);

    static class UpdateProfileHandler extends Handler {
        private WeakReference<EditProfileDialog> _editProfileDialog;

        public UpdateProfileHandler(EditProfileDialog editProfileDialog) {
            _editProfileDialog = new WeakReference<EditProfileDialog>(editProfileDialog);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity.getInstance().dismissWaitDialog();
            String jsonResults = (String) msg.obj;
            Log.d(LOG_TAG, "Update profile handler: " + msg);

            if (msg.what == AylaNetworks.AML_ERROR_OK) {
                // Update the owner contact information
                ContactManager cm = SessionManager.getInstance().getContactManager();
                AylaContact ownerContact = cm.getOwnerContact();

                if ( ownerContact == null ) {
                    Log.e(LOG_TAG, "No owner contact found! Creating...");
                    cm.createOwnerContact();
                    _editProfileDialog.get().dismiss();
                    Toast.makeText(MainActivity.getInstance(), R.string.profile_updated, Toast.LENGTH_LONG).show();
                } else {
                    ownerContact.phoneNumber = _editProfileDialog.get()._phoneNumber.getText().toString();
                    ownerContact.firstName = _editProfileDialog.get()._firstName.getText().toString();
                    ownerContact.lastName = _editProfileDialog.get()._lastName.getText().toString();
                    ownerContact.zipCode = _editProfileDialog.get()._zip.getText().toString();
                    ownerContact.country = _editProfileDialog.get()._country.getText().toString();
                    ownerContact.phoneCountryCode = AylaUser.getCurrent().phoneCountryCode;
                    ownerContact.displayName = ownerContact.firstName + " " + ownerContact.lastName;

                    cm.updateContact(ownerContact, new ContactManager.ContactManagerListener() {
                        @Override
                        public void contactListUpdated(ContactManager manager, boolean succeeded) {
                            _editProfileDialog.get().dismiss();
                            Toast.makeText(MainActivity.getInstance(),
                                    succeeded ? R.string.profile_updated : R.string.error_changing_profile,
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            } else {
                String errMsg = null;
                if (msg.arg1 == AylaNetworks.AML_USER_INVALID_PARAMETERS) {
                    AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "E", "amca.signin", "errors", jsonResults, "userSignUp");

                    // In the error case, the returned aylaUser will contain an error message in the field that had an error.
                    AylaUser aylaUser = AylaSystemUtils.gson.fromJson(jsonResults, AylaUser.class);
                    if (aylaUser.firstname != null) {
                        _editProfileDialog.get()._firstName.requestFocus();
                        errMsg = _editProfileDialog.get()._firstName.getHint() + " " + aylaUser.firstname;
                    } else if (aylaUser.lastname != null) {
                        _editProfileDialog.get()._lastName.requestFocus();
                        errMsg = _editProfileDialog.get()._lastName.getHint() + " " + aylaUser.lastname;
                    } else if (aylaUser.phone != null) {
                        _editProfileDialog.get()._phoneNumber.requestFocus();
                        errMsg = _editProfileDialog.get()._phoneNumber.getHint() + " " + aylaUser.phone;
                    } else if (aylaUser.zip != null) {
                        _editProfileDialog.get()._zip.requestFocus();
                        errMsg = _editProfileDialog.get()._zip.getHint() + " " + aylaUser.zip;
                    } else if (aylaUser.country != null) {
                        _editProfileDialog.get()._country.requestFocus();
                        errMsg = _editProfileDialog.get()._country.getHint() + " " + aylaUser.country;
                    }

                    if (errMsg != null) {
                        Toast.makeText(MainActivity.getInstance(), errMsg, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(MainActivity.getInstance(), R.string.error_changing_profile, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(MainActivity.getInstance(), R.string.error_changing_profile, Toast.LENGTH_LONG).show();
                }
            }
        }
    }
    private UpdateProfileHandler _updateProfileHandler = new UpdateProfileHandler(this);
}

