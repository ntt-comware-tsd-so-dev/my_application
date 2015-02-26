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
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;

import com.aylanetworks.aaml.AylaScheduleAction;
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
    private TextView _scheduleHelpTextView;
    private Switch _scheduleEnabledSwitch;
    private ScrollView _scrollView;
    private RadioGroup _repeatingRadioGroup;
    private LinearLayout _fullScheduleLayout;
    private LinearLayout _timerScheduleLayout;
    private TimePicker _scheduleTimePicker;
    private TimePicker _timerTimePicker;
    private Button _onTimeButton;
    private Button _offTimeButton;

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
        _scheduleTitleEditText = (EditText)root.findViewById(R.id.schedule_title_edittext);
        _scheduleHelpTextView = (TextView)root.findViewById(R.id.schedule_help_textview);
        _scheduleEnabledSwitch = (Switch)root.findViewById(R.id.schedule_enabled_switch);
        _scrollView = (ScrollView)root.findViewById(R.id.schedule_scroll_view);
        _repeatingRadioGroup = (RadioGroup)root.findViewById(R.id.repeating_radio_group);
        _fullScheduleLayout = (LinearLayout)root.findViewById(R.id.complex_schedule_layout);
        _timerScheduleLayout = (LinearLayout)root.findViewById(R.id.schedule_timer_layout);
        _scheduleTimePicker = (TimePicker)root.findViewById(R.id.timer_duration_picker);
        _timerTimePicker = (TimePicker)root.findViewById(R.id.time_on_off_picker);
        _onTimeButton = (Button)root.findViewById(R.id.button_turn_on);
        _offTimeButton = (Button)root.findViewById(R.id.button_turn_off);


        // Control configuration / setup
        _scheduleEnabledSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                scheduleEnabledChanged((Switch) buttonView, isChecked);
            }
        });

        _repeatingRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                repeatChanged(checkedId);
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
                    _repeatingRadioGroup.requestFocus();
                    return true;
                }
                return false;
            }
        });

        _onTimeButton.setSelected(true);
        _onTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _onTimeButton.setSelected(true);
                _offTimeButton.setSelected(false);
                onOffChanged(true);
            }
        });

        _offTimeButton.setSelected(false);
        _offTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _onTimeButton.setSelected(false);
                _offTimeButton.setSelected(true);
                onOffChanged(false);
            }
        });

        _scheduleTimePicker.setIs24HourView(true);

        int day = 1;
        for ( int id : _weekdayButtonIDs ) {
            final Button b = (Button)root.findViewById(id);
            b.setTag(day++);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Set<Integer> selectedDays = _schedule.getDaysOfWeek();
                    if ( selectedDays.contains(v.getTag())) {
                        b.setSelected(false);
                        selectedDays.remove(v.getTag());
                        _schedule.setDaysOfWeek(selectedDays);
                    } else {
                        b.setSelected(true);
                        selectedDays.add((Integer)v.getTag());
                        _schedule.setDaysOfWeek(selectedDays);
                    }
                }
            });
        }

        updateUI();
        return root;
    }

    private void onOffChanged(boolean isOn) {

    }

    private void repeatChanged(int checkedId) {
        updateUI();
    }

    private void scheduleEnabledChanged(Switch scheduleEnabledSwitch, boolean isChecked) {
        _schedule.setActive(isChecked);
        updateUI();
    }

    private boolean isOneShot() {
        if ( _schedule == null ) {
            return false;
        }

        if ( _schedule.getInterval() > 0 ) {
            return false;
        }

        if ( _schedule.getEndDate() == null ) {
            return false;
        }

        return true;
    }

    private void updateUI() {
        // Make the UI reflect the schedule for this device
        if ( _schedule == null || !_schedule.isActive() ) {
            // Nothing shown except for the switch
            _scrollView.setVisibility(View.GONE);
            return;
        }

        _scrollView.setVisibility(View.VISIBLE);
        int checkedId = _repeatingRadioGroup.getCheckedRadioButtonId();
        if ( checkedId == -1 ) {
            _repeatingRadioGroup.check(R.id.radio_timer);
        }

        if ( _repeatingRadioGroup.getCheckedRadioButtonId() == R.id.radio_timer ) {
            _timerScheduleLayout.setVisibility(View.VISIBLE);
            _fullScheduleLayout.setVisibility(View.GONE);
        } else {
            _timerScheduleLayout.setVisibility(View.GONE);
            _fullScheduleLayout.setVisibility(View.VISIBLE);
        }

        // Update the selected days of week
        for ( int i = 0; i < 7; i++ ) {
            Button b = (Button)_fullScheduleLayout.findViewById(_weekdayButtonIDs[i]);
            Set<Integer> days = _schedule.getDaysOfWeek();
            Log.d(LOG_TAG, "Days: " + days);
            b.setSelected(_schedule.getDaysOfWeek().contains(i + 1));
        }

        // Update the time pickers
        Calendar startTime = _schedule.getStartTimeEachDay();

        _timerTimePicker.setCurrentHour(startTime.get(Calendar.HOUR));
        _timerTimePicker.setCurrentMinute(startTime.get(Calendar.MINUTE));

        if ( !turnOnIsSelected() ) {
            // Add the schedule duration
            startTime.add(Calendar.MINUTE, _schedule.getDuration());
        }

        _scheduleTimePicker.setCurrentHour(startTime.get(Calendar.HOUR));
        _scheduleTimePicker.setCurrentMinute(startTime.get(Calendar.MINUTE));
    }

    private boolean turnOnIsSelected() {
        return _onTimeButton.isSelected();
    }
}
