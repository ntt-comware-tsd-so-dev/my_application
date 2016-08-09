package com.aylanetworks.agilelink.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.aylanetworks.agilelink.framework.AMAPCore;
import com.aylanetworks.aylasdk.AylaLog;
import com.aylanetworks.aylasdk.AylaServiceApp;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.AccountSettings;
import com.aylanetworks.agilelink.framework.PushNotification;
import com.aylanetworks.aylasdk.error.AylaError;

/*
 * DeviceNotificationsFragment.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 2/17/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class DeviceNotificationsFragment extends Fragment implements CompoundButton.OnCheckedChangeListener {
    private static final String LOG_TAG = "NotificationsFragment";

    private CheckBox _emailCheckbox;
    private CheckBox _smsCheckbox;
    private CheckBox _pushCheckbox;

    public static DeviceNotificationsFragment newInstance() {
        return new DeviceNotificationsFragment();
    }

    public DeviceNotificationsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        _emailCheckbox = (CheckBox)view.findViewById(R.id.checkbox_email);
        _emailCheckbox.setOnCheckedChangeListener(this);

        _smsCheckbox = (CheckBox)view.findViewById(R.id.checkbox_sms);
        _smsCheckbox.setOnCheckedChangeListener(this);

        _pushCheckbox = (CheckBox)view.findViewById(R.id.checkbox_push);
        _pushCheckbox.setOnCheckedChangeListener(this);

        _pushCheckbox.setEnabled(PushNotification.registrationId != null);

        // Get our account settings
        MainActivity.getInstance().showWaitDialog(R.string.updating_notifications_title, R.string.updating_notifications_body);
        AccountSettings.fetchAccountSettings(new UpdateSettingsCallback(true));

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Log.d(LOG_TAG, "onCheckedChanged: " + buttonView.getId());
        switch ( buttonView.getId() ) {
            case R.id.checkbox_email:
                enableNotification(AylaServiceApp.NotificationType.EMail, isChecked);
                break;

            case R.id.checkbox_sms:
                enableNotification(AylaServiceApp.NotificationType.SMS, isChecked);
                break;

            case R.id.checkbox_push:
                enableNotification(AylaServiceApp.NotificationType.GooglePush, isChecked);
                break;

            default:
                Log.e(LOG_TAG, "Unknown check from view: " + buttonView);
                break;
        }
    }

    private void updateCheckboxes() {
        enableCheckboxListeners(false);
        AccountSettings accountSettings = AMAPCore.sharedInstance().getAccountSettings();
        if ( accountSettings == null ) {
            // Nothing checked
            _emailCheckbox.setChecked(false);
            _smsCheckbox.setChecked(false);
            _pushCheckbox.setChecked(false);
        } else {
            _emailCheckbox.setChecked(accountSettings.isNotificationMethodSet(AylaServiceApp
                    .NotificationType.EMail));
            _smsCheckbox.setChecked(accountSettings.isNotificationMethodSet(AylaServiceApp
                    .NotificationType.SMS));
            _pushCheckbox.setChecked(accountSettings.isNotificationMethodSet(AylaServiceApp
                    .NotificationType.GooglePush));
        }

        enableCheckboxListeners(true);
    }

    private void enableCheckboxListeners(boolean enable) {
        CompoundButton.OnCheckedChangeListener listener = enable ? this : null;
        _emailCheckbox.setOnCheckedChangeListener(listener);
        _smsCheckbox.setOnCheckedChangeListener(listener);
        _pushCheckbox.setOnCheckedChangeListener(listener);
    }

    private void enableNotification(final AylaServiceApp.NotificationType notificationMethod, final
    boolean enable) {
        Log.d(LOG_TAG, "Email notifications: " + enable);
        AccountSettings accountSettings = AMAPCore.sharedInstance().getAccountSettings();
        if ( accountSettings == null ) {
            // Fetch the account settings now
            MainActivity.getInstance().showWaitDialog(R.string.fetching_account_info_title, R.string.fetching_account_info_body);
            AccountSettings.fetchAccountSettings(new AccountSettings.AccountSettingsCallback() {
                public void settingsUpdated(AccountSettings settings, Message msg) {
                    MainActivity.getInstance().dismissWaitDialog();
                    if ( settings != null ) {
                        enableNotification(notificationMethod, enable);
                    } else {
                        Toast.makeText(MainActivity.getInstance(), R.string.unknown_error, Toast.LENGTH_SHORT).show();
                    }
                }
            });
            return;
        }

        if ( enable ) {
            accountSettings.addNotificationMethod(notificationMethod);
        } else {
            accountSettings.removeNotificationMethod(notificationMethod);
        }

        MainActivity.getInstance().showWaitDialog(R.string.updating_notifications_title,
                R.string.updating_notifications_body);

        accountSettings.pushToServer(new UpdateSettingsCallback(false));
        updateNotifications(notificationMethod, enable);
    }

    private void updateNotifications(AylaServiceApp.NotificationType notificationType, boolean enable) {
        AMAPCore.sharedInstance().updateDeviceNotifications(
                notificationType,
                enable,
                new AMAPCore.DeviceNotificationListener() {
                    @Override
                    public void notificationsUpdated(AylaError error) {
                        MainActivity.getInstance().dismissWaitDialog();
                        if (error == null) {
                            Toast.makeText(getActivity(), R.string.notifications_updated, Toast.LENGTH_SHORT).show();
                        } else {
                            AylaLog.e(LOG_TAG, "Failed updating notifications: " + error);
                            Toast.makeText(getActivity(), R.string.notification_update_failed, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private class UpdateSettingsCallback extends AccountSettings.AccountSettingsCallback {
        private boolean _dismissDialogWhenDone;
        public UpdateSettingsCallback(boolean dismissDialogWhenDone) {
            _dismissDialogWhenDone = dismissDialogWhenDone;
        }
        public void settingsUpdated(AccountSettings settings, Message msg) {
            updateCheckboxes();
            if ( _dismissDialogWhenDone ) {
                MainActivity.getInstance().dismissWaitDialog();
            }
        }
    }
}
