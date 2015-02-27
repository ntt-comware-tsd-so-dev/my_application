package com.aylanetworks.agilelink.fragments;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;

import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.Schedule;
import com.aylanetworks.agilelink.framework.SessionManager;

import java.util.Calendar;
import java.util.Set;

/**
 * Fragment for presenting scheduling UI
 */
public class ScheduleFragment extends Fragment {
    private final static String LOG_TAG = "ScheduleFragment";

    private final static String ARG_DEVICE_DSN = "deviceDSN";
    private final static String ARG_SCHEDULE_INDEX = "scheduleIndex";

    private Device _device;
    private Schedule _schedule;

    private EditText _scheduleTitleEditText;
    private Switch _scheduleEnabledSwitch;
    private ScrollView _scrollView;
    private RadioGroup _scheduleTypeRadioGroup;
    private LinearLayout _fullScheduleLayout;
    private RelativeLayout _scheduleDetailsLayout;
    private LinearLayout _timerScheduleLayout;
    private TimePicker _scheduleTimePicker;
    private TimePicker _timerTimePicker;

    // On / off time buttons for the repeating schedule
    private Button _scheduleOnTimeButton;
    private Button _scheduleOffTimeButton;

    // On off time buttons for the timer
    private Button _timerTurnOnButton;
    private Button _timerTurnOffButton;

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

    static ScheduleFragment newInstance(Device device, int scheduleIndex) {
        Bundle args = new Bundle();
        args.putString(ARG_DEVICE_DSN, device.getDevice().dsn);
        args.putInt(ARG_SCHEDULE_INDEX, scheduleIndex);
        ScheduleFragment frag = new ScheduleFragment();
        frag.setArguments(args);

        return frag;
    }

    public ScheduleFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Get our device argument
        _device = SessionManager.deviceManager().deviceByDSN(getArguments().getString(ARG_DEVICE_DSN));
        int scheduleIndex = getArguments().getInt(ARG_SCHEDULE_INDEX);
        _schedule = _device.getSchedules().get(scheduleIndex);

        Log.e(LOG_TAG, "onCreateView(" + _schedule.getName() + ")");

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

        _timerTurnOnButton = (Button) root.findViewById(R.id.timer_turn_on_button);
        _timerTurnOffButton = (Button) root.findViewById(R.id.timer_turn_off_button);

        // Control configuration / setup
        _scheduleEnabledSwitch.setChecked(_schedule.isActive());
        _scheduleEnabledSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                scheduleEnabledChanged((Switch) buttonView, isChecked);
            }
        });
        scheduleEnabledChanged(_scheduleEnabledSwitch, _scheduleEnabledSwitch.isChecked());

        _scheduleTypeRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                updateUI();
            }
        });

        _scheduleTitleEditText.setText(_schedule.getName());
        _scheduleTitleEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_NULL) {
                    // Save the schedule name and dismiss the keyboard
                    _schedule.setName(v.getText().toString());
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(_scheduleTitleEditText.getWindowToken(), 0);
                    _scheduleTypeRadioGroup.requestFocus();
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

        updateUI();
        return root;
    }

    private void scheduleTimeChanged(int hourOfDay, int minute) {
        if ( _updatingUI ) {
            return;
        }

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);

        if (_scheduleOnTimeButton.isSelected()) {
            _schedule.setOnTime(cal);
        } else {
            _schedule.setOffTime(cal);
        }
    }

    private void timerTimeChanged(int hourOfDay, int minute) {
        if ( _updatingUI ) {
            return;
        }

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR_OF_DAY, hourOfDay);
        cal.add(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);

        if (_timerTurnOnButton.isSelected()) {
            _schedule.setOnTime(cal);
        } else {
            _schedule.setOffTime(cal);
        }
    }

    private void scheduleEnabledChanged(Switch scheduleEnabledSwitch, boolean isChecked) {
        _schedule.setActive(isChecked);
        _scheduleDetailsLayout.setVisibility((isChecked ? View.VISIBLE : View.GONE));
        updateUI();
    }

    private void updateUI() {
        // Make the UI reflect the schedule for this device
        if (_schedule == null || !_schedule.isActive()) {
            // Nothing shown except for the switch
            return;
        }

        _updatingUI = true;

        int checkedId = _scheduleTypeRadioGroup.getCheckedRadioButtonId();
        if (checkedId == -1) {
            _scheduleTypeRadioGroup.check(R.id.radio_timer);
        }

        if (_scheduleTypeRadioGroup.getCheckedRadioButtonId() == R.id.radio_timer) {
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
            Log.d(LOG_TAG, "Days: " + days);
            b.setSelected(_schedule.getDaysOfWeek().contains(i + 1));
        }

        // Update the pickers
        Calendar pickerTime;
        Calendar now = Calendar.getInstance();

        // First the timer picker
        if (_timerTurnOnButton.isSelected()) {
            pickerTime = _schedule.getOnTime();
            if (pickerTime != null) {
                pickerTime.add(Calendar.HOUR_OF_DAY, -now.get(Calendar.HOUR_OF_DAY));
                pickerTime.add(Calendar.MINUTE, -now.get(Calendar.MINUTE));
            }
        } else {
            pickerTime = _schedule.getOffTime();
            if (pickerTime != null) {
                pickerTime.add(Calendar.HOUR_OF_DAY, -now.get(Calendar.HOUR_OF_DAY));
                pickerTime.add(Calendar.MINUTE, -now.get(Calendar.MINUTE));
            }
        }

        if (pickerTime == null) {
            pickerTime = Calendar.getInstance();
            pickerTime.set(Calendar.HOUR_OF_DAY, 0);
            pickerTime.set(Calendar.MINUTE, 0);
        }

        _timerTimePicker.setCurrentHour(pickerTime.get(Calendar.HOUR_OF_DAY));
        _timerTimePicker.setCurrentMinute(pickerTime.get(Calendar.MINUTE));

        // Now the schedule picker
        if (_scheduleOnTimeButton.isSelected()) {
            // Add the schedule duration
            pickerTime = _schedule.getOnTime();
        } else {
            pickerTime = _schedule.getOffTime();
        }

        if (pickerTime == null) {
            // Set to the on or off time if set, or the current time if not set
            if ( _scheduleOnTimeButton.isSelected() ) {
                pickerTime = _schedule.getOffTime();
            } else {
                pickerTime = _schedule.getOnTime();
            }

            if ( pickerTime == null ) {
                pickerTime = Calendar.getInstance();
            }
        }

        _scheduleTimePicker.setCurrentHour(pickerTime.get(Calendar.HOUR_OF_DAY));
        _scheduleTimePicker.setCurrentMinute(pickerTime.get(Calendar.MINUTE));

        _updatingUI = false;
    }
}
