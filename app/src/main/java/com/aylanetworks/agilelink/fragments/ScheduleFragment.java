package com.aylanetworks.agilelink.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.Schedule;
import com.aylanetworks.agilelink.framework.SessionManager;

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

        // Control configuration / setup
        _scheduleEnabledSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                scheduleEnabledChanged((Switch)buttonView, isChecked);
            }
        });

        _repeatingRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                repeatChanged(checkedId);
            }
        });

        _scheduleTitleEditText.setText(String.format(getString(R.string.schedule_title),
                _device.toString(), _schedule.getName()));

        updateUI();
        return root;
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
    }
}
