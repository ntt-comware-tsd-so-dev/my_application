/*
 * SharesFragment.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 3/26/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

package com.aylanetworks.agilelink.fragments;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.SessionManager;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * This class presents an interface to the user for sharing devices. The fragment can be configured
 * to present a list of devices to share, or can be set with a specific device to share ahead of
 * time. When the user has entered all of the necessary information for sharing, the listener
 * is notified with the results.
 * <p/>
 * This class does not itself set up the sharing, but rather provides the UI for doing so.
 */
public class ShareDevicesFragment extends Fragment {
    private final static String LOG_TAG = "ShareDevicesFragment";
    private ShareDevicesListener _listener;
    private Calendar _shareStartDate;
    private Calendar _shareEndDate;
    private boolean _readOnly;
    private Device _device;             // Only set if we're sharing exactly one device

    public interface ShareDevicesListener {
        /**
         * When the user taps the "Share" button, this listener method will be called with
         * information about the shares. This method will also be called if the user does not have
         * any devices that can be shared. All fields will be null / false in that case.
         *
         * @param email          Email address of the user to share devices with, or null if canceled
         * @param startDate      Date the sharing begins, or null if none selected
         * @param endDate        Date the sharing ends, or null if none selected
         * @param readOnly       Set to true if the share can not be controlled by the recipient
         * @param devicesToShare A list of devices to be shared, or null if the user canceled
         */
        void shareDevices(String email, Calendar startDate, Calendar endDate, boolean readOnly, List<Device> devicesToShare);
    }

    /**
     * Creates an instance of the ShareDevicesFragment.
     * <p/>
     * This version of this method should be used to present a list of devices to be shared.
     *
     * @param listener Listener to receive the sharing information
     * @return the ShareDevicesFragment ready to be shown.
     */
    public static ShareDevicesFragment newInstance(ShareDevicesListener listener) {
        return newInstance(listener, null);
    }

    /**
     * Creates an instance of the ShareDevicesFragment. This fragment is a DialogFragment, and
     * should be launched via the {@link #show(android.support.v4.app.FragmentManager, String)}
     * method after creation.
     * <p/>
     * This version of this method should be used when the device to be shared is known ahead of
     * time (e.g. from the Device Details page). If the user should be presented with a list of
     * devices to select from for sharing (e.g. from the Sharing page in Settings), then the
     * device parameter should be null or {@link #newInstance(com.aylanetworks.agilelink.fragments.ShareDevicesFragment.ShareDevicesListener)}
     * should be used instead of this method.
     *
     * @param listener Listener to receive the sharing information
     * @param device   Device to be shared, or null to present a list of devices in the dialog
     * @return
     */
    public static ShareDevicesFragment newInstance(ShareDevicesListener listener, Device device) {
        ShareDevicesFragment frag = new ShareDevicesFragment();
        frag._listener = listener;
        frag._device = device;
        return frag;
    }

    private final static DateFormat _dateFormat;
    static {
        _dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
    }

    private ListView _deviceList;
    private EditText _email;
    private RadioGroup _radioGroup;
    private Button _startButton;
    private Button _endButton;
    private Button _shareButton;

    // Layout only shown if a device was passed in to newInstance()
    private LinearLayout _deviceLayout;
    private ImageView _deviceImageView;
    private TextView _deviceTextView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_share_devices, null);
        _deviceList = (ListView) root.findViewById(R.id.share_listview);
        _email = (EditText) root.findViewById(R.id.share_email);
        _radioGroup = (RadioGroup) root.findViewById(R.id.read_only_radio_group);
        _startButton = (Button)root.findViewById(R.id.button_starting_on);
        _endButton = (Button)root.findViewById(R.id.button_ending_on);
        _shareButton = (Button) root.findViewById(R.id.share_button);
        _deviceImageView = (ImageView)root.findViewById(R.id.device_image);
        _deviceLayout = (LinearLayout)root.findViewById(R.id.device_layout);
        _deviceTextView = (TextView)root.findViewById(R.id.device_name);

        _startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseDate((Button)v);
            }
        });

        _endButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseDate((Button)v);
            }
        });

        if ( _device != null ) {
            // We have a specific device we're sharing. Don't show the list of devices- just show
            // the device name / icon
            _deviceList.setVisibility(View.GONE);
            _deviceLayout.setVisibility(View.VISIBLE);
            _deviceTextView.setText(_device.toString());
            _deviceImageView.setImageDrawable(_device.getDeviceDrawable(MainActivity.getInstance()));
        } else {
            // Set up the list view with a set of devices that the owner can share
            _deviceList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
            List<Device> deviceList = SessionManager.deviceManager().deviceList();
            // Remove devices that we don't own from this list
            List<Device> filteredList = new ArrayList<Device>();
            for (Device d : deviceList) {
                if (d.getDevice().amOwner()) {
                    filteredList.add(d);
                }
            }

            if (filteredList.isEmpty()) {
                Toast.makeText(getActivity(), R.string.no_devices_to_share, Toast.LENGTH_LONG).show();
                if (_listener != null) {
                    // Nothing to share.
                    _listener.shareDevices(null, null, null, false, null);
                    return root;
                }
            }

            Device devices[] = deviceList.toArray(new Device[deviceList.size()]);
            _deviceList.setAdapter(new ArrayAdapter<Device>(inflater.getContext(), android.R.layout.simple_list_item_multiple_choice, devices));
        }

        _shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareDevices();
            }
        });

        return root;
    }

    private void chooseDate(final Button button) {
        Calendar now = Calendar.getInstance();
        if ( _shareStartDate == null ) {
            _shareStartDate = Calendar.getInstance();
            _shareStartDate.setTimeInMillis(0);
        }
        if ( _shareEndDate == null ) {
            _shareEndDate = Calendar.getInstance();
            _shareEndDate.setTimeInMillis(0);
        }

        final Calendar dateToModify = (button.getId() == R.id.button_starting_on ? _shareStartDate : _shareEndDate);
        DatePickerDialog d = new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Log.d(LOG_TAG, "Date: " + year + "/" + monthOfYear + "/" + dayOfMonth);
                dateToModify.set(Calendar.YEAR, year);
                dateToModify.set(Calendar.MONTH, monthOfYear);
                dateToModify.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateButtonText();
            }
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
        d.setButton(DialogInterface.BUTTON_NEUTRAL, getResources().getString(R.string.no_date),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        dateToModify.setTimeInMillis(0);
                        updateButtonText();
                    }
                });
        d.show();
    }

    private void updateButtonText() {
        if ( _shareStartDate == null || _shareStartDate.getTimeInMillis() == 0 ) {
            _startButton.setText(R.string.now);
        } else {
            _startButton.setText(_dateFormat.format(_shareStartDate.getTime()));
        }

        if ( _shareEndDate == null || _shareEndDate.getTimeInMillis() == 0 ) {
            _endButton.setText(R.string.never);
        } else {
            _endButton.setText(_dateFormat.format(_shareEndDate.getTime()));
        }
    }

    private void shareDevices() {
        List<Device> devicesToAdd = new ArrayList<Device>();

        if ( _device != null ) {
            // This is the only device we care about
            devicesToAdd.add(_device);
        } else {
            // Get the selected devices from the listview
            SparseBooleanArray checkedItems = _deviceList.getCheckedItemPositions();
            for (int i = 0; i < _deviceList.getAdapter().getCount(); i++) {
                if (checkedItems.get(i)) {
                    Device device = (Device) _deviceList.getAdapter().getItem(i);
                    devicesToAdd.add(device);
                }
            }
            String email = _email.getText().toString();
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(getActivity(), R.string.share_email_address_required, Toast.LENGTH_LONG).show();
                return;
            }

            if (devicesToAdd.isEmpty()) {
                Toast.makeText(getActivity(), R.string.no_devices_to_share, Toast.LENGTH_LONG).show();
                return;
            }
        }

        if ( _shareStartDate != null && _shareStartDate.getTimeInMillis() == 0 ) {
            _shareStartDate = null;
        }
        if ( _shareEndDate != null && _shareEndDate.getTimeInMillis() == 0 ) {
            _shareEndDate = null;
        }

        _readOnly = (_radioGroup.getCheckedRadioButtonId() == R.id.radio_read_only);

        Log.d(LOG_TAG, "Add Shares: " + devicesToAdd);
        _listener.shareDevices(
                _email.getText().toString(),
                _shareStartDate,
                _shareEndDate,
                _readOnly,
                devicesToAdd);
    }
}