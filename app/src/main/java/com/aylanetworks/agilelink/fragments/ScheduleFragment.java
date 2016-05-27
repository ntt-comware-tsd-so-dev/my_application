package com.aylanetworks.agilelink.fragments;


import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.Schedule;
import com.aylanetworks.agilelink.framework.SessionManager;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

/*
 * ScheduleFragment.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 2/17/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class ScheduleFragment extends Fragment {
    private final static String LOG_TAG = "ScheduleFragment";

    private final static String ARG_DEVICE_DSN = "deviceDSN";
    private final static String ARG_SCHEDULE_INDEX = "scheduleIndex";

    private Device _device;
    private Schedule _schedule;
    private int _scheduleIndex;

    private EditText _scheduleTitleEditText;
    private Switch _scheduleEnabledSwitch;
    private ScrollView _scrollView;
    private RadioGroup _scheduleTypeRadioGroup;
    private LinearLayout _fullScheduleLayout;
    private RelativeLayout _scheduleDetailsLayout;
    private LinearLayout _timerScheduleLayout;
    private TimePicker _scheduleTimePicker;
    private TimePicker _timerTimePicker;
    private Button _saveScheduleButton;

    private LinearLayout _propertySelectionLayout;
    private LinearLayout _propertySelectionCheckboxLayout;

    // On / off time buttons for the repeating schedule
    private Button _scheduleOnTimeButton;
    private Button _scheduleOffTimeButton;

    // On off time buttons for the timer
    private Button _timerTurnOnButton;
    private Button _timerTurnOffButton;

    private int _timerOnDuration;
    private int _timerOffDuration;

    private boolean _updatingUI;

    private static final int[] _weekdayButtonIDs = {
            R.id.button_sunday,
            R.id.button_monday,
            R.id.button_tuesday,
            R.id.button_wednesday,
            R.id.button_thursday,
            R.id.button_friday,
            R.id.button_saturday
    };

    public static ScheduleFragment newInstance(Device device, int scheduleIndex) {
        Bundle args = new Bundle();
        args.putString(ARG_DEVICE_DSN, device.getDeviceDsn());
        args.putInt(ARG_SCHEDULE_INDEX, scheduleIndex);
        ScheduleFragment frag = new ScheduleFragment();
        frag.setArguments(args);

        return frag;
    }

    public ScheduleFragment() {
        _timerOnDuration = 60;
        _timerOffDuration = 120;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Get our device argument
        _device = SessionManager.deviceManager().deviceByDSN(getArguments().getString(ARG_DEVICE_DSN));
        _scheduleIndex = getArguments().getInt(ARG_SCHEDULE_INDEX);

        // Make a copy to work with.
        try {
            List<Schedule> schedules = _device.getSchedules();
            if ( schedules != null ) {
                _schedule = schedules.get(_scheduleIndex).clone();
            }
        } catch (CloneNotSupportedException | NullPointerException e) {
            e.printStackTrace();
            MainActivity.getInstance().popBackstackToRoot();
            Toast.makeText(getActivity(), R.string.unknown_error, Toast.LENGTH_LONG).show();
            return new View(getActivity());
        }
        if(_schedule == null){
            Log.d(LOG_TAG, "onCreateView _schedule is null ");
            MainActivity.getInstance().popBackstackToRoot();
            Toast.makeText(getActivity(), R.string.unknown_error, Toast.LENGTH_LONG).show();
            return new View(getActivity());
        }

        Log.d(LOG_TAG, "onCreateView(" + _schedule.getName() + ")");

        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_schedule, container, false);

        // Get our views set up
        _scheduleTitleEditText = (EditText) root.findViewById(R.id.schedule_title_edittext);
        _scheduleEnabledSwitch = (Switch) root.findViewById(R.id.schedule_enabled_switch);
        _scrollView = (ScrollView) root.findViewById(R.id.schedule_scroll_view);
        _scheduleTypeRadioGroup = (RadioGroup) root.findViewById(R.id.schedule_type_radio_group);
        _fullScheduleLayout = (LinearLayout) root.findViewById(R.id.complex_schedule_layout);
        _timerScheduleLayout = (LinearLayout) root.findViewById(R.id.schedule_timer_layout);
        _scheduleDetailsLayout = (RelativeLayout) root.findViewById(R.id.schedule_details_layout);
        _scheduleTimePicker = (TimePicker) root.findViewById(R.id.time_on_off_picker);
        _timerTimePicker = (TimePicker) root.findViewById(R.id.timer_duration_picker);
        _scheduleOnTimeButton = (Button) root.findViewById(R.id.button_turn_on);
        _scheduleOffTimeButton = (Button) root.findViewById(R.id.button_turn_off);

        _propertySelectionLayout = (LinearLayout) root.findViewById(R.id.property_selection_layout);
        _propertySelectionCheckboxLayout = (LinearLayout) root.findViewById(R.id.property_selection_checkbox_layout);

        _timerTurnOnButton = (Button) root.findViewById(R.id.timer_turn_on_button);
        _timerTurnOffButton = (Button) root.findViewById(R.id.timer_turn_off_button);

        _saveScheduleButton = (Button)root.findViewById(R.id.save_schedule);

        // Control configuration / setup
        _scheduleEnabledSwitch.setChecked(_schedule.isActive());
        _scheduleEnabledSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                scheduleEnabledChanged(isChecked);
            }
        });
        scheduleEnabledChanged(_scheduleEnabledSwitch.isChecked());


        _scheduleTypeRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if ( _updatingUI ) {
                    return;
                }
                _schedule.clearSchedule();
                _schedule.setIsTimer(checkedId == R.id.radio_timer);
                updateUI();
            }
        });

        _scheduleTitleEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                _schedule.setName(s.toString());
            }
        });

        _scheduleTitleEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_NULL) {
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(_scheduleTitleEditText.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });

        _scheduleOnTimeButton.setSelected(true);
        _scheduleOnTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _scheduleOnTimeButton.setSelected(true);
                _scheduleOffTimeButton.setSelected(false);
                updateUI();
            }
        });

        _scheduleOffTimeButton.setSelected(false);
        _scheduleOffTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _scheduleOnTimeButton.setSelected(false);
                _scheduleOffTimeButton.setSelected(true);
                updateUI();
            }
        });

        _scheduleTimePicker.setIs24HourView(false);
        _scheduleTimePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                Log.d(LOG_TAG, "onTimeChanged: " + hourOfDay + ":" + minute + "(" + view.getCurrentHour() + ":" + view.getCurrentMinute());
                scheduleTimeChanged(hourOfDay, minute);
            }
        });

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            initSchedule(_scheduleTimePicker.getCurrentHour()
                    , _scheduleTimePicker.getCurrentMinute());
        } else { // version >= M
            initSchedule(_scheduleTimePicker.getHour()
                    , _scheduleTimePicker.getMinute());
        }

        _timerTimePicker.setIs24HourView(true);
        _timerTimePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                timerTimeChanged(hourOfDay, minute);
            }
        });
        _timerTurnOnButton.setSelected(true);
        _timerTurnOnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _timerTurnOnButton.setSelected(true);
                _timerTurnOffButton.setSelected(false);
                updateUI();
            }
        });
        _timerTurnOffButton.setSelected(false);
        _timerTurnOffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _timerTurnOnButton.setSelected(false);
                _timerTurnOffButton.setSelected(true);
                updateUI();
            }
        });

        // Set up the buttons for weekdays
        int day = 1;
        for (int id : _weekdayButtonIDs) {
            final Button b = (Button) root.findViewById(id);
            b.setTag(day++);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Set<Integer> selectedDays = _schedule.getDaysOfWeek();
                    if (selectedDays.contains((Integer) v.getTag())) {
                        b.setSelected(false);
                        selectedDays.remove(v.getTag());
                        _schedule.setDaysOfWeek(selectedDays);
                    } else {
                        b.setSelected(true);
                        selectedDays.add((Integer) v.getTag());
                        _schedule.setDaysOfWeek(selectedDays);
                    }
                }
            });
        }

        _saveScheduleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSchedule();
            }
        });

        setupPropertySelection();

        updateUI();
        return root;
    }

    TimeZone getTimeZone() {
        TimeZone tz = _device.getTimeZone();
        if (tz == null) {
            tz = TimeZone.getTimeZone("UTC");
        }
        return tz;
    }

    private void scheduleTimeChanged(int hourOfDay, int minute) {
        if ( _updatingUI ) {
            return;
        }

        setSchedule(hourOfDay, minute, _scheduleOnTimeButton.isSelected());
    }

    private void initSchedule(int hourOfDay, int minute) {
        setSchedule(hourOfDay, minute, true);
        setSchedule(hourOfDay, minute+1, false);
    }

    private void setSchedule(int hourOfDay, int minute, boolean isOnTimeButtonSelected) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(getTimeZone());
        cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);

        if (isOnTimeButtonSelected) {
            _schedule.setStartTimeEachDay(cal);
        } else {
            _schedule.setEndTimeEachDay(cal);
        }
        _schedule.updateScheduleActions();
    }

    private void timerTimeChanged(int hourOfDay, int minute) {
        if ( _updatingUI ) {
            return;
        }

        if (_timerTurnOnButton.isSelected()) {
            _timerOnDuration = hourOfDay * 60 + minute;
        } else {
            _timerOffDuration = hourOfDay * 60 + minute;
        }
    }

    private void scheduleEnabledChanged(boolean isChecked) {
        _schedule.setActive(isChecked);
        _scheduleDetailsLayout.setVisibility((isChecked ? View.VISIBLE : View.GONE));
        updateUI();
    }

    private boolean checkSchedule() {
        int errorMessage = 0;
        if ( _schedule.isActive() && _schedule.getActions().size() == 0 ) {
           errorMessage = R.string.no_actions_set;
        }

        if ( !_schedule.isTimer() && _schedule.getStartTimeEachDay() == null) {
            errorMessage = R.string.configure_a_timer_or_a_schedule_before_saving;
        }

        if ( errorMessage != 0 ) {
            Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_LONG).show();
        }

        return errorMessage == 0;
    }

    private void saveSchedule() {
        // Make sure that all required fields are set
        if ( !checkSchedule() ) {
            return;
        }

        MainActivity.getInstance().showWaitDialog(R.string.updating_schedule_title, R.string.updating_schedule_body);

        // Start date is always "right now".
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(getTimeZone());
        _schedule.setStartDate(cal);


        if ( _schedule.isTimer() ) {
            // Set up the schedule
            _schedule.setTimer(_timerOnDuration, _timerOffDuration);
        } else { // now cloud side forces endDate validation
            _schedule.setEndDate(cal);
        }

        Log.d(LOG_TAG, "start: " + _schedule.getSchedule().startDate);
        Log.d(LOG_TAG, "end:   " + _schedule.getSchedule().endDate);

        // Copy our local schedule copy over to the device's schedule object
        Schedule s = _device.getSchedules().get(_scheduleIndex);
        _schedule.cloneTo(s);

        // Save the updated schedule
        _device.updateSchedule(s, new Device.DeviceStatusListener() {
            @Override
            public void statusUpdated(Device device, boolean changed) {
                MainActivity.getInstance().dismissWaitDialog();
                if (changed) {
                    Toast.makeText(getActivity(), R.string.schedule_updated, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), R.string.schedule_update_failed, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void setupPropertySelection() {
        _propertySelectionCheckboxLayout.removeAllViewsInLayout();

        Map<String, String> enableActions = new HashMap<>();
        Map<String, String> disableActions = new HashMap<>();
        int nSelected = 0;
        CheckBox firstCheckBox = null;
        String[] propertyNames = _device.getSchedulablePropertyNames();
        for ( String propertyName : propertyNames ) {
            CheckBox cb = new CheckBox(getActivity());
            cb.setText(_device.friendlyNameForPropertyName(propertyName));
            cb.setTag(propertyName);
            if ( _schedule.isPropertyActive(propertyName) ) {
                cb.setChecked(true);
                _schedule.addAction(propertyName);
                nSelected++;
            }
            cb.setTextAppearance(getActivity(), android.R.style.TextAppearance_Medium);
            cb.setTextColor(getResources().getColor(R.color.enabled_text));
            cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    propertySelectionChanged((CheckBox) buttonView, isChecked);
                }
            });
            if ( firstCheckBox == null ) {
                firstCheckBox = cb;
            }
            _propertySelectionCheckboxLayout.addView(cb);
        }

        // If we only have one choice, don't even bother displaying the UI
        if ( propertyNames.length == 1 ) {
            _propertySelectionLayout.setVisibility(View.GONE);
            _schedule.addAction(propertyNames[0]);
        } else {
            _propertySelectionLayout.setVisibility(View.VISIBLE);
            if ( nSelected == 0 && firstCheckBox != null ) {
                // Check the first box
                firstCheckBox.setChecked(true);
            }
        }

        _schedule.updateScheduleActions();
        _propertySelectionLayout.requestLayout();
    }

    private void propertySelectionChanged(CheckBox cb, boolean isChecked) {
        String propertyName = (String)cb.getTag();
        Log.d(LOG_TAG, "Property selection changed: " + propertyName);

        if ( isChecked ) {
            _schedule.addAction(propertyName);
        } else {
            _schedule.removeAction(propertyName);
        }
        _schedule.updateScheduleActions();
    }

    private void updateUI() {
        // Make the UI reflect the schedule for this device
        if ( _schedule != null ) {
            _scheduleTitleEditText.setText(_schedule.getName());
        }

        if (_schedule == null || !_schedule.isActive()) {
            // Nothing shown except for the switch
            return;
        }

        _updatingUI = true;

        int checkedId = _schedule.isTimer() ? R.id.radio_timer : R.id.radio_repeating;
        _scheduleTypeRadioGroup.check(checkedId);

        if (checkedId == R.id.radio_timer) {
            _timerScheduleLayout.setVisibility(View.VISIBLE);
            _fullScheduleLayout.setVisibility(View.GONE);
        } else {
            _timerScheduleLayout.setVisibility(View.GONE);
            _fullScheduleLayout.setVisibility(View.VISIBLE);
        }

        // Update the selected days of week
        for (int i = 0; i < 7; i++) {
            Button b = (Button) _fullScheduleLayout.findViewById(_weekdayButtonIDs[i]);
            Set<Integer> days = _schedule.getDaysOfWeek();
            b.setSelected(_schedule.getDaysOfWeek().contains(i + 1));
        }

        // Update the pickers
        Calendar pickerTime;
        Calendar now = Calendar.getInstance();
        now.setTimeZone(TimeZone.getTimeZone("UTC"));

        // First the timer picker
        int hour, minute;
        if ( _schedule.isTimer() ) {
            if (_timerTurnOnButton.isSelected()) {
                hour = (_timerOnDuration / 60);
                minute = (_timerOnDuration % 60);
            } else {
                hour = (_timerOffDuration / 60);
                minute = (_timerOffDuration % 60);
            }

            _timerTimePicker.setCurrentHour(hour);
            _timerTimePicker.setCurrentMinute(minute);
        } else {
            // The schedule picker
            // Set to the on or off time if set, or the current time if not set
            if (_scheduleOnTimeButton.isSelected()) {
                pickerTime = _schedule.getStartTimeEachDay();
            } else {
                pickerTime = _schedule.getEndTimeEachDay();
            }

            if (pickerTime == null) {
                pickerTime = Calendar.getInstance();
            }

            _scheduleTimePicker.setCurrentHour(pickerTime.get(Calendar.HOUR_OF_DAY));
            _scheduleTimePicker.setCurrentMinute(pickerTime.get(Calendar.MINUTE));
        }

        _updatingUI = false;
    }
}
