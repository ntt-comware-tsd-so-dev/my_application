package com.aylanetworks.agilelink.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.aylanetworks.aaml.AylaContact;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.aaml.AylaUser;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.ContactManager;
import com.aylanetworks.agilelink.framework.Logger;
import com.aylanetworks.agilelink.framework.MenuHandler;
import com.aylanetworks.agilelink.framework.SessionManager;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/*
 * EditProfileFragment.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 2/4.15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class EditProfileFragment extends Fragment implements View.OnClickListener {
    private final static String LOG_TAG = "EditProfileDialog";

    private EditText _firstName;
    private EditText _lastName;
    private EditText _email;
    private EditText _newEmail;
    private EditText _country;
    private EditText _phoneCountryCode;
    private EditText _phoneNumber;

    private EditText _oldPassword;
    private EditText _password;
    private EditText _confirmPassword;

    public static EditProfileFragment newInstance() {
        return new EditProfileFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.account_details, container, false);

        root.findViewById(R.id.btnUpdate).setOnClickListener(this);
        root.findViewById(R.id.btnChangePassword).setOnClickListener(this);
        root.findViewById(R.id.btnLogOut).setOnClickListener(this);
        root.findViewById(R.id.btnDeleteAccount).setOnClickListener(this);
        root.findViewById(R.id.btnUpdateEmail).setOnClickListener(this);

        _firstName = (EditText) root.findViewById(R.id.etFirstName);
        _lastName = (EditText) root.findViewById(R.id.etLastName);
        _email = (EditText) root.findViewById(R.id.etEmail);
        _newEmail = (EditText) root.findViewById(R.id.etNewEmail);
        _country = (EditText) root.findViewById(R.id.etCountry);
        _phoneCountryCode = (EditText) root.findViewById(R.id.etPhoneCountryCode);
        _phoneNumber = (EditText) root.findViewById(R.id.etPhoneNumber);

        _oldPassword = (EditText) root.findViewById(R.id.etCurrentPassword);
        _password = (EditText) root.findViewById(R.id.etNewPassword);
        _confirmPassword = (EditText) root.findViewById(R.id.etConfirmPassword);

        _email.setEnabled(false);

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Update our current user's information
        String title = MainActivity.getInstance().getResources().getString(R.string.fetching_user_info_title);
        String body = MainActivity.getInstance().getResources().getString(R.string.fetching_user_info_body);
        MainActivity.getInstance().showWaitDialog(title, body);
        Log.d(LOG_TAG, "user: AylaUser.getInfo started");
        AylaUser.getInfo(_getInfoHandler);
    }

    @Override
    public void onPause() {
        MainActivity.getInstance().dismissWaitDialog();
        super.onPause();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        super.onPrepareOptionsMenu(menu);
    }

    private void updateFields() {
        AylaUser currentUser = AylaUser.getCurrent();
        _firstName.setText(currentUser.firstname);
        _lastName.setText(currentUser.lastname);
        _email.setText(currentUser.email);
        _country.setText(currentUser.country);
        _phoneCountryCode.setText(currentUser.phoneCountryCode);
        _phoneNumber.setText(currentUser.phone);
        _password.setText("");
        _confirmPassword.setText("");
    }

    private Map<String, String> getParameters() {
        Map<String, String> params = new HashMap<>();
        params.put("firstname", _firstName.getText().toString());
        params.put("lastname", _lastName.getText().toString());
        params.put("country", _country.getText().toString());
        params.put("phone_country_code", _phoneCountryCode.getText().toString());
        params.put("phone", _phoneNumber.getText().toString());
        return params;
    }

    /*
    Add Editable fields for the Identity Provider
     */
    private Map<String, String> getSSOUserParameters() {
        Map<String, String> params = new HashMap<>();
        params.put("email", _email.getText().toString());
        params.put("firstname", _firstName.getText().toString());
        params.put("lastname", _lastName.getText().toString());
        params.put("country", _country.getText().toString());
        params.put("phone", _phoneNumber.getText().toString());
        return params;
    }

    void onUpdateClicked() {
        // Normal profile update
        MainActivity.getInstance().showWaitDialog(getString(R.string.updating_profile_title), getString(R.string.updating_profile_body));
        SessionManager.SessionParameters params = SessionManager.sessionParameters();
        if(params.ssoLogin){
            params.ssoManager.updateUserInfo(_ssoUpdateProfileHandler, getSSOUserParameters());
        }else{
            AylaUser.updateInfo(_updateProfileHandler, getParameters(), params.appId, params.appSecret);
        }

        AylaUser.updateInfo(_updateProfileHandler, getParameters(), params.appId, params.appSecret);
    }

    void onChangePasswordClicked() {
        String currentPassword = _oldPassword.getText().toString();
        String newPassword = _password.getText().toString();
        String confirm = _confirmPassword.getText().toString();
        if (TextUtils.isEmpty(currentPassword)) {
            _oldPassword.requestFocus();
            Toast.makeText(MainActivity.getInstance(), R.string.password_required, Toast.LENGTH_SHORT).show();
        } else if (!TextUtils.equals(newPassword, confirm)) {
            _password.setText("");
            _confirmPassword.setText("");
            _password.requestFocus();
            Toast.makeText(MainActivity.getInstance(), R.string.password_no_match, Toast.LENGTH_SHORT).show();
        } else if (newPassword.length() < 6) {
            _password.requestFocus();
            Toast.makeText(MainActivity.getInstance(), R.string.password_too_short, Toast.LENGTH_SHORT).show();
        } else {
            // Update the password
            MainActivity.getInstance().showWaitDialog(getString(R.string.updating_profile_title), getString(R.string.updating_profile_body));
            AylaUser.changePassword(_changePasswordHandler, currentPassword, newPassword);
        }
    }

    void onUpdateEmailClicked() {
        String currentEmail = _email.getText().toString();
        final String newEmail = _newEmail.getText().toString();
        if (TextUtils.isEmpty(newEmail) || newEmail.contains("@") == false
                || currentEmail.equals(newEmail)) {
            Toast.makeText(MainActivity.getInstance(), R.string.invalid_email, Toast.LENGTH_SHORT).show();
            _newEmail.requestFocus();
        } else {
            new AlertDialog.Builder(MainActivity.getInstance())
                    .setIcon(R.drawable.ic_launcher)
                    .setTitle(R.string.update_email_confirm_title)
                    .setMessage(R.string.update_email_confirm_msg)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Update the email
                            MainActivity.getInstance().showWaitDialog(R.string.updating_email_title, R.string.updating_email_body);
                            AylaUser.updateEmailAddress(_updateEmailHandler, newEmail);
                        }
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .create().show();
        }
    }

    void onLogOutClicked() {
        MenuHandler.signOut();
    }

    void onDeleteAccountClicked() {
        MenuHandler.deleteAccount();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnUpdate:
                onUpdateClicked();
                break;
            case R.id.btnChangePassword:
                onChangePasswordClicked();
                break;
            case R.id.btnLogOut:
                onLogOutClicked();
                break;
            case R.id.btnDeleteAccount:
                onDeleteAccountClicked();
                break;
            case R.id.btnUpdateEmail:
                onUpdateEmailClicked();
                break;
        }
    }

    static class ChangePasswordHandler extends Handler {
        private WeakReference<EditProfileFragment> _editProfileDialog;
        public ChangePasswordHandler(EditProfileFragment editProfileFragment) {
            _editProfileDialog = new WeakReference<EditProfileFragment>(editProfileFragment);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(LOG_TAG, "Change password handler: " + msg);
            String jsonResults = (String) msg.obj;

            if (AylaNetworks.succeeded(msg)) {
                SessionManager.SessionParameters params = SessionManager.sessionParameters();
                params.password = _editProfileDialog.get()._password.getText().toString();

                // Clear out the password edit fields so we don't try to set the password again
                _editProfileDialog.get()._password.setText("");
                _editProfileDialog.get()._confirmPassword.setText("");

                // Continue updating the rest of the information
                _editProfileDialog.get().onClick(_editProfileDialog.get().getView().findViewById(R.id.btnUpdate));
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
        private WeakReference<EditProfileFragment> _editProfileDialog;

        public GetInfoHandler(EditProfileFragment editProfileFragment) {
            _editProfileDialog = new WeakReference<EditProfileFragment>(editProfileFragment);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(LOG_TAG, "user: GetInfo handleMessage [" + msg + "]");
            Logger.logMessage(LOG_TAG, msg, "GetInfoHandler");
            MainActivity.getInstance().dismissWaitDialog();
            if (AylaNetworks.succeeded(msg)) {
                AylaUser user = AylaSystemUtils.gson.fromJson((String) msg.obj, AylaUser.class);
                Log.d(LOG_TAG, "user: " + user);
                // Save the auth info- it's not filled out in the returned user object
                AylaUser oldUser = AylaUser.getCurrent();
                user.setAccessToken(oldUser.getAccessToken());
                user.setRefreshToken(oldUser.getRefreshToken());
                user.setExpiresIn(oldUser.getExpiresIn());
                AylaUser.setCurrent(user);
            } else {
                Log.e(LOG_TAG, "user: Failed to fetch current user details: " + msg);
            }
            _editProfileDialog.get().updateFields();
        }
    }

    private GetInfoHandler _getInfoHandler = new GetInfoHandler(this);

    static class UpdateEmailHandler extends Handler {
        private WeakReference<EditProfileFragment> _editProfileFragment;

        public UpdateEmailHandler(EditProfileFragment editProfileFragment) {
            _editProfileFragment = new WeakReference<EditProfileFragment>(editProfileFragment);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(LOG_TAG, "user: UpdateEmail handleMessage [" + msg + "]");
            Logger.logMessage(LOG_TAG, msg, "UpdateEmail");
            MainActivity.getInstance().dismissWaitDialog();
            if (AylaNetworks.succeeded(msg)) {
                Toast.makeText(MainActivity.getInstance(), R.string.update_email_success, Toast.LENGTH_SHORT).show();
                // Once change email, sign in with new email is required
                SessionManager.stopSession();
            } else {
                Log.e(LOG_TAG, "user: Failed to update email: " + msg);
            }
        }
    }

    private UpdateEmailHandler _updateEmailHandler = new UpdateEmailHandler(this);

    static class UpdateProfileHandler extends Handler {
        private WeakReference<EditProfileFragment> _editProfileDialog;

        public UpdateProfileHandler(EditProfileFragment editProfileFragment) {
            _editProfileDialog = new WeakReference<>(editProfileFragment);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity.getInstance().dismissWaitDialog();
            String jsonResults = (String) msg.obj;
            Log.d(LOG_TAG, "Update profile handler: " + msg);

            if (AylaNetworks.succeeded(msg)) {
                // Update the owner contact information
                ContactManager cm = SessionManager.getInstance().getContactManager();
                AylaContact ownerContact = cm.getOwnerContact();

                if ( ownerContact == null ) {
                    Log.e(LOG_TAG, "No owner contact found! Creating...");
                    cm.createOwnerContact();
                    _editProfileDialog.get().getFragmentManager().popBackStack();
                    Toast.makeText(MainActivity.getInstance(), R.string.profile_updated, Toast.LENGTH_LONG).show();
                } else {
                    ownerContact.firstname = _editProfileDialog.get()._firstName.getText().toString();
                    ownerContact.lastname = _editProfileDialog.get()._lastName.getText().toString();
                    ownerContact.phoneCountryCode = _editProfileDialog.get()._phoneCountryCode.getText().toString();
                    ownerContact.phoneNumber = _editProfileDialog.get()._phoneNumber.getText().toString();
                    ownerContact.country = _editProfileDialog.get()._country.getText().toString();
                    ownerContact.displayName = ownerContact.firstname + " " + ownerContact.lastname;
                    ContactManager.normalizePhoneNumber(ownerContact);

                    cm.updateContact(ownerContact, new ContactManager.ContactManagerListener() {
                        @Override
                        public void contactListUpdated(ContactManager manager, boolean succeeded) {
                            if(_editProfileDialog.get().getFragmentManager() != null){
                                _editProfileDialog.get().getFragmentManager().popBackStack();
                            }
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
                    } else if (aylaUser.phoneCountryCode != null) {
                        _editProfileDialog.get()._phoneCountryCode.requestFocus();
                        errMsg = _editProfileDialog.get()._phoneCountryCode.getHint() + " " + aylaUser.phoneCountryCode;
                    } else if (aylaUser.phone != null) {
                        _editProfileDialog.get()._phoneNumber.requestFocus();
                        errMsg = _editProfileDialog.get()._phoneNumber.getHint() + " " + aylaUser.phone;
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


    /**
     * Handler for updating profile on Identity provider's service
     * To be modified by app developer
     */
    static class SsoUpdateProfileHandler extends Handler {
        private WeakReference<EditProfileFragment> _editProfileDialog;

        public SsoUpdateProfileHandler(EditProfileFragment editProfileFragment) {
            _editProfileDialog = new WeakReference<EditProfileFragment>(editProfileFragment);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity.getInstance().dismissWaitDialog();
            String jsonResults = (String) msg.obj;
            Log.d(LOG_TAG, "SSO Update profile handler: " + msg);

            if (msg.arg1 >= 200 && msg.arg1 <300)  {
                // Update the owner contact information
                ContactManager cm = SessionManager.getInstance().getContactManager();
                AylaContact ownerContact = cm.getOwnerContact();

                if ( ownerContact == null ) {
                    Log.d(LOG_TAG, "No owner contact found! Creating...");
                    cm.createOwnerContact();
                    _editProfileDialog.get().getFragmentManager().popBackStack();
                    Toast.makeText(MainActivity.getInstance(), R.string.profile_updated, Toast.LENGTH_LONG).show();
                } else {
                    ownerContact.firstname = _editProfileDialog.get()._firstName.getText().toString();
                    ownerContact.lastname = _editProfileDialog.get()._lastName.getText().toString();
                    ownerContact.phoneCountryCode = _editProfileDialog.get()._phoneCountryCode.getText().toString();
                    ownerContact.phoneNumber = _editProfileDialog.get()._phoneNumber.getText().toString();
                    ownerContact.country = _editProfileDialog.get()._country.getText().toString();
                    ownerContact.displayName = ownerContact.firstname + " " + ownerContact.lastname;
                    ContactManager.normalizePhoneNumber(ownerContact);

                    cm.updateContact(ownerContact, new ContactManager.ContactManagerListener() {
                        @Override
                        public void contactListUpdated(ContactManager manager, boolean succeeded) {
                            _editProfileDialog.get().getFragmentManager().popBackStack();
                            Toast.makeText(MainActivity.getInstance(),
                                    succeeded ? R.string.profile_updated : R.string.error_changing_profile,
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            } else {
                if (msg.arg1 == AylaNetworks.AML_ERROR_UNREACHABLE) {
                    AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "E", "amca.signin", "errors", jsonResults, "userSignUp");
                    if (jsonResults != null) {
                        Toast.makeText(MainActivity.getInstance(), jsonResults, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(MainActivity.getInstance(), R.string.error_changing_profile, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(MainActivity.getInstance(), R.string.error_changing_profile, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private SsoUpdateProfileHandler _ssoUpdateProfileHandler = new SsoUpdateProfileHandler(this);
}

