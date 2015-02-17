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

import com.aylanetworks.aaml.AylaUser;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.AccountSettings;
import com.aylanetworks.agilelink.framework.DeviceNotification;

/**
 * Notifications Fragment
 * Allows the user to choose email, SMS and push as notification types
 */
public class NotificationsFragment extends Fragment implements CompoundButton.OnCheckedChangeListener {
    private static final String LOG_TAG = "NotificationsFragment";
    private AccountSettings _accountSettings;

    private CheckBox _emailCheckbox;
    private CheckBox _smsCheckbox;
    private CheckBox _pushCheckbox;

    public NotificationsFragment() {
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

        // Get our account settings
        MainActivity.getInstance().showWaitDialog(R.string.updating_notifications_title, R.string.updating_notifications_body);
        AccountSettings.fetchAccountSettings(AylaUser.getCurrent(), new UpdateSettingsCallback());

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
                enableEmailNotification(isChecked);
                break;

            case R.id.checkbox_sms:
                enableSMSNotification(isChecked);
                break;

            case R.id.checkbox_push:
                enablePushNotification(isChecked);
                break;

            default:
                Log.e(LOG_TAG, "Unknown check from view: " + buttonView);
                break;
        }
    }

    private void updateCheckboxes() {
        enableCheckboxListeners(false);
        if ( _accountSettings == null ) {
            // Nothing checked
            _emailCheckbox.setChecked(false);
            _smsCheckbox.setChecked(false);
            _pushCheckbox.setChecked(false);
        } else {
            _emailCheckbox.setChecked(_accountSettings.isNotificationMethodSet(DeviceNotification.NOTIFICATION_METHOD_EMAIL));
            _smsCheckbox.setChecked(_accountSettings.isNotificationMethodSet(DeviceNotification.NOTIFICATION_METHOD_SMS));
            _pushCheckbox.setChecked(_accountSettings.isNotificationMethodSet(DeviceNotification.NOTIFICATION_METHOD_PUSH));
        }
        enableCheckboxListeners(true);
    }

    private void enableCheckboxListeners(boolean enable) {
        CompoundButton.OnCheckedChangeListener listener = enable ? this : null;
        _emailCheckbox.setOnCheckedChangeListener(listener);
        _smsCheckbox.setOnCheckedChangeListener(listener);
        _pushCheckbox.setOnCheckedChangeListener(listener);
    }

    private void enableEmailNotification(boolean enable) {
        Log.d(LOG_TAG, "Email notifications: " + enable);

        if ( enable ) {
            _accountSettings.addNotificationType(DeviceNotification.NOTIFICATION_METHOD_EMAIL);
        } else {
            _accountSettings.removeNotificationType(DeviceNotification.NOTIFICATION_METHOD_EMAIL);
        }

        MainActivity.getInstance().showWaitDialog(R.string.updating_notifications_title,
                R.string.updating_notifications_body);

        _accountSettings.pushToServer(new UpdateSettingsCallback());
    }

    private void enableSMSNotification(boolean enable) {
        Log.d(LOG_TAG, "SMS notifications: " + enable);
        if ( enable ) {
            _accountSettings.addNotificationType(DeviceNotification.NOTIFICATION_METHOD_SMS);
        } else {
            _accountSettings.removeNotificationType(DeviceNotification.NOTIFICATION_METHOD_SMS);
        }

        MainActivity.getInstance().showWaitDialog(R.string.updating_notifications_title,
                R.string.updating_notifications_body);

        _accountSettings.pushToServer(new UpdateSettingsCallback());
    }

    private void enablePushNotification(boolean enable) {
        Log.d(LOG_TAG, "Push notifications: " + enable);
    }

    private class UpdateSettingsCallback extends AccountSettings.AccountSettingsCallback {
        @Override
        public void settingsUpdated(AccountSettings settings, Message msg) {
            _accountSettings = settings;
            updateCheckboxes();
            MainActivity.getInstance().dismissWaitDialog();
        }
    }
}
