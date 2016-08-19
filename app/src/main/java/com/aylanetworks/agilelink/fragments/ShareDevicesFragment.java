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
import android.util.TypedValue;
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
import com.aylanetworks.agilelink.framework.AMAPCore;
import com.aylanetworks.agilelink.framework.ViewModel;
import com.aylanetworks.aylasdk.AylaDevice;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * This class presents an interface to the user for sharing devices. The fragment can be configured
 * to present a list of devices to share, or can be set with a specific device to share ahead of
 * time. When the user has entered all of the necessary information for sharing, the listener
 * is notified with the results.
 * <p>
 * This class does not itself set up the sharing, but rather provides the UI for doing so.
 * </p>
 */
public class ShareDevicesFragment extends Fragment implements View.OnFocusChangeListener {

    private final static String LOG_TAG = "ShareDevicesFragment";
    private ShareDevicesListener _listener;
    private Calendar _shareStartDate;
    private Calendar _shareEndDate;
    private AylaDevice _device; // Only set if we're sharing exactly one device

    public interface ShareDevicesListener {
        /**
         * When the user taps the "Share" button, this listener method will be called with
         * information about the shares. This method will also be called if the user does not have
         * any devices that can be shared. All fields will be null / false in that case.
         *
         * @param email          Email address of the user to share devices with, or null if canceled
         * @param role           Enable the role based sharing feature, or null for regular sharing
         * @param startDate      Date the sharing begins, or null if none selected
         * @param endDate        Date the sharing ends, or null if none selected
         * @param readOnly       Set to true if the share can not be controlled by the recipient
         * @param devicesToShare A list of devices to be shared, or null if the user canceled
         */
        void shareDevices(String email, String role, Calendar startDate, Calendar endDate, boolean
                readOnly, List<AylaDevice> devicesToShare);
    }

    /**
     * Creates an instance of the ShareDevicesFragment.
     * <p>
     * This version of this method should be used to present a list of devices to be shared.
     *</p>
     * @param listener Listener to receive the sharing information
     * @return the ShareDevicesFragment
     */
    public static ShareDevicesFragment newInstance(ShareDevicesListener listener) {
        return newInstance(listener, null);
    }

    /**
     * Creates an instance of the ShareDevicesFragment.
     * <p>
     * This version of this method should be used when the device to be shared is known ahead of
     * time (e.g. from the Device Details page). If the user should be presented with a list of
     * devices to select from for sharing (e.g. from the Sharing page in Settings), then the
     * device parameter should be null or {@link #newInstance(com.aylanetworks.agilelink.fragments.ShareDevicesFragment.ShareDevicesListener)}
     * should be used instead of this method.
     * </p>
     * @param listener Listener to receive the sharing information
     * @param device   Device to be shared, or null to present a list of devices in the dialog
     * @return the new ShareDevicesFragment
     */
    public static ShareDevicesFragment newInstance(ShareDevicesListener listener,
                                                   AylaDevice device) {
        ShareDevicesFragment frag = new ShareDevicesFragment();
        frag._listener = listener;
        frag._device = device;
        return frag;
    }

    private final static DateFormat _dateFormat;
    static {
        _dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
    }

    private TextView _Instructions;
    private ListView _deviceList;
    private EditText _email;
    private EditText _role;
    private RadioGroup _radioGroup;
    private Button _startButton;
    private Button _endButton;

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
        _Instructions = (TextView) root.findViewById(R.id.instructions);
        _deviceList = (ListView) root.findViewById(R.id.share_listview);
        _email = (EditText) root.findViewById(R.id.share_email);
        _role = (EditText) root.findViewById(R.id.share_role);

        _email.setOnFocusChangeListener(this);
        _role.setOnFocusChangeListener(this);

        _radioGroup = (RadioGroup) root.findViewById(R.id.read_only_radio_group);
        _startButton = (Button)root.findViewById(R.id.button_starting_on);
        _endButton = (Button)root.findViewById(R.id.button_ending_on);
        Button shareButton = (Button) root.findViewById(R.id.share_button);
        ImageView deviceImageView = (ImageView)root.findViewById(R.id.device_image);
        LinearLayout deviceLayout = (LinearLayout)root.findViewById(R.id.device_layout);
        TextView deviceTextView = (TextView)root.findViewById(R.id.device_name);
        TextView shareInfoText = (TextView)root.findViewById(R.id.textview);

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
            deviceTextView.setText(_device.getFriendlyName());
            ViewModel deviceModel = AMAPCore.sharedInstance().getSessionParameters().viewModelProvider
                    .viewModelForDevice(_device);
            deviceImageView.setImageDrawable(deviceModel.getDeviceDrawable(MainActivity.getInstance()));

            if (_device.isLanEnabled()){
                _radioGroup.setVisibility(View.INVISIBLE);
                _radioGroup.check(R.id.radio_read_write);
                shareInfoText.setText(getResources().getString(R.string.share_control_only_message));
            } else {
                _radioGroup.setVisibility(View.VISIBLE);
                _radioGroup.check(R.id.radio_read_only);
                shareInfoText.setText(getResources().getString(R.string.share_read_only_message));
            }
        } else {
            // Set up the list view with a set of devices that the owner can share
            deviceLayout.setVisibility(View.GONE);
            _deviceList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
            List<AylaDevice> deviceList = AMAPCore.sharedInstance().getDeviceManager().getDevices();
            // Remove devices that we don't own from this list
            List<AylaDevice> filteredList = new ArrayList<AylaDevice>();
            for (AylaDevice d : deviceList) {
                if (d.getGrant() == null) {
                    filteredList.add(d);
                }
            }

            if (filteredList.isEmpty()) {
                Toast.makeText(getActivity(), R.string.no_devices_to_share, Toast.LENGTH_LONG).show();
                if (_listener != null) {
                    // Nothing to share.
                    _listener.shareDevices(null, null, null, null, false, null);
                    return root;
                }
            }

            AylaDevice devices[] = deviceList.toArray(new AylaDevice[deviceList.size()]);
            _deviceList.setAdapter(new ArrayAdapter<>(inflater.getContext(), android.R.layout.simple_list_item_multiple_choice, devices));
            int unitHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48,
                    this.getActivity().getResources().getDisplayMetrics());
            _deviceList.getLayoutParams().height = devices.length * unitHeight;
        }

        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareDevices();
            }
        });

        return root;
    }

    private void chooseDate(final Button button) {
        Calendar now = Calendar.getInstance();
        final int id = button.getId();

        DatePickerDialog d = new DatePickerDialog(getActivity(),
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        if(id == R.id.button_starting_on){
                            if(_shareStartDate == null){
                                _shareStartDate = Calendar.getInstance();
                            }
                            _shareStartDate.set(Calendar.YEAR, year);
                            _shareStartDate.set(Calendar.MONTH, monthOfYear);
                            _shareStartDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        } else{
                            if(_shareEndDate == null){
                                _shareEndDate = Calendar.getInstance();
                            }
                            _shareEndDate.set(Calendar.YEAR, year);
                            _shareEndDate.set(Calendar.MONTH, monthOfYear);
                            _shareEndDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        }
                        updateButtonText();

                    }
                }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
        d.setButton(DialogInterface.BUTTON_NEUTRAL, getResources().getString(R.string.no_date),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if(id == R.id.button_start_date){
                            _shareStartDate  = null;
                        } else{
                            _shareEndDate = null;
                        }
                        updateButtonText();
                    }
                });
        d.getDatePicker().setMinDate(now.getTimeInMillis());
        d.show();
    }

    private void updateButtonText() {
        if ( _shareStartDate == null) {
            _startButton.setText(R.string.now);
        } else {
            _startButton.setText(_dateFormat.format(_shareStartDate.getTime()));
        }

        if ( _shareEndDate == null) {
            _endButton.setText(R.string.never);
        } else {
            _endButton.setText(_dateFormat.format(_shareEndDate.getTime()));
        }
    }

    private void shareDevices() {
        List<AylaDevice> devicesToAdd = new ArrayList<>();

        if ( _device != null ) {
            // This is the only device we care about
            if(!_device.isGateway()){
                devicesToAdd.add(_device);
            } else {
                Toast.makeText(getActivity(), "Sharing of Gateways not supported", Toast.LENGTH_LONG).show();
                return;
            }
        } else {
            // Get the selected devices from the listview
            SparseBooleanArray checkedItems = _deviceList.getCheckedItemPositions();
            for (int i = 0; i < _deviceList.getAdapter().getCount(); i++) {
                if (checkedItems.get(i)) {
                    AylaDevice device = (AylaDevice) _deviceList.getAdapter().getItem(i);
                    if(!device.isGateway()){
                        devicesToAdd.add(device);
                    } else {
                        Toast.makeText(getActivity(), "Sharing of Gateways not supported", Toast.LENGTH_LONG).show();
                        return;
                    }
                }
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

        boolean readOnly = (_radioGroup.getCheckedRadioButtonId() == R.id.radio_read_only);

        Log.d(LOG_TAG, "Add Shares: " + devicesToAdd);
        _listener.shareDevices(
                _email.getText().toString(),
                _role.getText().toString(),
                _shareStartDate,
                _shareEndDate,
                readOnly,
                devicesToAdd);
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if ( v == _email) {
            _Instructions.setText(R.string.share_instructions);
            return;
        }

        if ( v == _role) {
            _Instructions.setText(R.string.add_role_share_message );
            return;
        }
    }// end of onFocusChange
}