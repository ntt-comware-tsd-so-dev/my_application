package com.aylanetworks.agilelink.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Response;
import com.aylanetworks.agilelink.ErrorUtils;
import com.aylanetworks.agilelink.framework.AMAPCore;
import com.aylanetworks.aylasdk.AylaAPIRequest;
import com.aylanetworks.aylasdk.AylaContact;
import com.aylanetworks.aylasdk.AylaLog;
import com.aylanetworks.aylasdk.AylaUser;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.ContactManager;
import com.aylanetworks.agilelink.MenuHandler;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;
import com.aylanetworks.aylasdk.util.EmptyListener;

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
    private EditText _city;
    private EditText _zip;

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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_edit_profile, menu);
        super.onCreateOptionsMenu(menu, inflater);
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
        _city = (EditText) root.findViewById(R.id.etCity);
        _zip = (EditText) root.findViewById(R.id.etZip);

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
        AMAPCore.sharedInstance().getSessionManager().fetchUserProfile(
                new Response.Listener<AylaUser>() {
                    @Override
                    public void onResponse(AylaUser response) {
                        AMAPCore.sharedInstance().setCurrentUser(response);
                        updateFields();
                        MainActivity.getInstance().dismissWaitDialog();
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        MainActivity.getInstance().dismissWaitDialog();
                        Toast.makeText(getActivity(),
                                ErrorUtils.getUserMessage(getActivity(), error, R.string.unknown_error),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onPause() {
        MainActivity.getInstance().dismissWaitDialog();
        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return MenuHandler.handleMenuId(item.getItemId());
    }

    private void updateFields() {
        AylaUser currentUser = AMAPCore.sharedInstance().getCurrentUser();
        _firstName.setText(currentUser.getFirstname());
        _lastName.setText(currentUser.getLastname());
        _email.setText(currentUser.getEmail());
        _country.setText(currentUser.getCountry());
        _phoneCountryCode.setText(currentUser.getPhoneCountryCode());
        _phoneNumber.setText(currentUser.getPhone());
        _city.setText(currentUser.getCity());
        _zip.setText(currentUser.getZip());
        _password.setText("");
        _confirmPassword.setText("");
    }

    private AylaUser userFromFields() {
        AylaUser user = new AylaUser();
        user.setFirstname(_firstName.getText().toString());
        user.setLastname(_lastName.getText().toString());
        user.setCountry(_country.getText().toString());
        user.setPhoneCountryCode(_phoneCountryCode.getText().toString());
        user.setPhone(_phoneNumber.getText().toString());
        user.setCity(_city.getText().toString());
        user.setZip(_zip.getText().toString());

        return user;
    }

    void onUpdateClicked() {
        // Normal profile update
        MainActivity.getInstance().showWaitDialog(getString(R.string.updating_profile_title), getString(R.string.updating_profile_body));
        AMAPCore.SessionParameters params = AMAPCore.sharedInstance().getSessionParameters();
        final AylaUser updatedUser = userFromFields();
        AMAPCore.sharedInstance().getSessionManager().updateUserProfile(updatedUser,
                new Response.Listener<AylaUser>() {
                    @Override
                    public void onResponse(AylaUser response) {
                        AylaLog.i(LOG_TAG, "User profile updated successfully");
                        updateOwnerContact(updatedUser);
                        MainActivity.getInstance().dismissWaitDialog();
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        Toast.makeText(getActivity(),
                                ErrorUtils.getUserMessage(getActivity(), error, R.string.unknown_error),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    void updateOwnerContact(AylaUser user) {
        ContactManager cm = AMAPCore.sharedInstance().getContactManager();
        AylaContact ownerContact = cm.getOwnerContact();

        if (ownerContact == null) {
            Log.e(LOG_TAG, "No owner contact found! Creating...");
            cm.createOwnerContact();
            getFragmentManager().popBackStack();
            Toast.makeText(MainActivity.getInstance(), R.string.profile_updated, Toast.LENGTH_LONG).show();
        } else {
            ownerContact.setFirstname(user.getFirstname());
            ownerContact.setLastname(user.getLastname());
            ownerContact.setPhoneCountryCode(user.getPhoneCountryCode());
            ownerContact.setPhoneNumber(user.getPhone());
            ownerContact.setCountry(user.getCountry());
            ownerContact.setDisplayName(ownerContact.getFirstname() + " " + ownerContact
                    .getLastname());
            ContactManager.normalizePhoneNumber(ownerContact);

            cm.updateContact(ownerContact, new ContactManager.ContactManagerListener() {
                @Override
                public void contactListUpdated(ContactManager manager, AylaError error) {
                    if (getFragmentManager() != null) {
                        getFragmentManager().popBackStack();
                    }
                    Toast.makeText(getActivity(),
                            ErrorUtils.getUserMessage(getActivity(), error,
                                    error == null ? R.string.profile_updated : R.string.error_changing_profile),
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
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
            AMAPCore.sharedInstance().getSessionManager().updatePassword(currentPassword, newPassword,
                    new Response.Listener<AylaAPIRequest.EmptyResponse>() {
                        @Override
                        public void onResponse(AylaAPIRequest.EmptyResponse response) {
                            Toast.makeText(MainActivity.getInstance(), R.string.update_password_success, Toast.LENGTH_SHORT).show();
                            // Re-login with new password
                            EmptyListener<AylaAPIRequest.EmptyResponse>
                                    emptyListener = new EmptyListener<>();
                            AMAPCore.sharedInstance().getSessionManager()
                                    .shutDown(emptyListener, emptyListener);
                            MainActivity.getInstance().showLoginDialog(true);
                        }
                    },
                    new ErrorListener() {
                        @Override
                        public void onErrorResponse(AylaError error) {
                            MainActivity.getInstance().dismissWaitDialog();
                            Toast.makeText(getActivity(),
                                    ErrorUtils.getUserMessage(getActivity(), error, R.string.error_changing_profile),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    void onUpdateEmailClicked() {
        String currentEmail = _email.getText().toString();
        final String newEmail = _newEmail.getText().toString();
        if (TextUtils.isEmpty(newEmail) || !newEmail.contains("@")
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
                            AMAPCore.sharedInstance().getSessionManager()
                                    .updateUserEmailAddress(newEmail, new Response.Listener<AylaAPIRequest.EmptyResponse>() {
                                                @Override
                                                public void onResponse(AylaAPIRequest.EmptyResponse response) {
                                                    Toast.makeText(MainActivity.getInstance(), R.string.update_email_success, Toast.LENGTH_SHORT).show();
                                                    // Once change email, sign in with new email is required
                                                    EmptyListener<AylaAPIRequest.EmptyResponse>
                                                            emptyListener = new EmptyListener<>();
                                                    AMAPCore.sharedInstance().getSessionManager()
                                                            .shutDown(emptyListener, emptyListener);
                                                    MainActivity.getInstance().showLoginDialog(true);
                                                }
                                            },
                                            new ErrorListener() {
                                                @Override
                                                public void onErrorResponse(AylaError error) {
                                                    Toast.makeText(getActivity(),
                                                            ErrorUtils.getUserMessage(getActivity(), error, R.string.error_updating_email),
                                                            Toast.LENGTH_SHORT).show();
                                                }
                                            });
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
}
