package com.aylanetworks.agilelink.framework;

import android.support.annotation.Nullable;
import android.util.Log;

import com.aylanetworks.aaml.AylaSchedule;
import com.aylanetworks.aaml.AylaScheduleAction;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

/*
 * DeviceGroup.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 2/23/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

/**
 * Class representing a Schedule for a device. This class wraps the AylaSchedule object and provides
 * convenience methods to manipulate, save and fetch schedule information for a device.
 * <p/>
 * This class supports two schedule "types", a recurring schedule and a timer. The recurring
 * schedule does not expire, and
 */
public class Schedule {
    private static String LOG_TAG = "Schedule";

    // The internal AylaSchedule object
    private AylaSchedule _schedule;

    // Maps of property names to their enable / disable action strings
    private Set<String> _actionProperties;

    /**
     * Creates a Schedule object initialized with the given AylaSchedule object
     *
     * @param schedule AylaSchedule used to initialize this Schedule object
     */
    public Schedule(AylaSchedule schedule) {
        _schedule = schedule;
        _schedule.utc = true;
        _actionProperties = new HashSet<>();
    }

    /**
     * Adds a property as an action for this schedule
     *
     * @param propertyName Name of the property to add to the schedule actions
     */
    public void addAction(String propertyName) {
        _actionProperties.add(propertyName);
    }

    /**
     * Removes a property as an action for this schedule
     *
     * @param propertyName Name of the property to remove from the schedule
     */
    public void removeAction(String propertyName) {
        _actionProperties.remove(propertyName);
    }

    /**
     * Sets the name of this schedule
     *
     * @param name The name for the schedule
     */
    public void setName(String name) {
        _schedule.displayName = name;
    }

    /**
     * Returns the name of the schedule
     *
     * @return the name of the schedule
     */
    public String getName() {
        return _schedule.displayName;
    }

    /**
     * Sets the schedule to be active or inactive
     *
     * @param active true to activate, false to deactivate
     */
    public void setActive(boolean active) {
        _schedule.active = active;
    }

    /**
     * Returns true if the schedule is active
     *
     * @return true if active, false if inactive
     */
    public boolean isActive() {
        return _schedule.active;
    }

    /**
     * Returns true if an action matching the propertyName is active.
     * @param propertyName Name of the property to check for activity
     * @return True if at least one action for the specified property is active
     */
    public boolean isPropertyActive(String propertyName) {
        if ( _schedule.scheduleActions == null ) {
            return false;
        }

        for ( AylaScheduleAction action : _schedule.scheduleActions ) {
            if ( action.name.equals(propertyName) && action.active ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sets the schedules start time each day
     *
     * @param startTime Time the schedule should start each day
     */
    public void setStartTimeEachDay(Calendar startTime) {
        if (startTime == null) {
            _schedule.startTimeEachDay = "";
        } else {
            _schedule.startTimeEachDay = _dateFormatHMS.format(startTime.getTime());
            Log.d(LOG_TAG, "setStartTimeEachDay: " + _schedule.startTimeEachDay);
        }
    }

    /**
     * Returns the schedule's start time each day
     *
     * @return A Calendar object containing the schedule's start time each day, or null if
     * the field is not valid
     */
    public Calendar getStartTimeEachDay() {
        Date date = null;
        try {
            date = _dateFormatHMS.parse(_schedule.startTimeEachDay);
        } catch (ParseException e) {
            Log.d(LOG_TAG, "Invalid date string (startTimeEachDay): " + _schedule.startTimeEachDay);
        }

        Calendar result = null;
        if (date != null) {
            result = today();
            result.setTime(date);
        }

        return result;
    }

    public void setEndTimeEachDay(Calendar endTime) {
        if (endTime == null) {
            _schedule.endTimeEachDay = null;
        } else {
            _schedule.endTimeEachDay = _dateFormatHMS.format(endTime.getTime());
            Log.d(LOG_TAG, "setEndTimeEachDay: " + _schedule.endTimeEachDay);
        }
    }

    public Calendar getEndTimeEachDay() {
        Date date = null;
        try {
            date = _dateFormatHMS.parse(_schedule.endTimeEachDay);
        } catch (ParseException e) {
            Log.d(LOG_TAG, "Invalid date string (endTimeEachDay): " + _schedule.endTimeEachDay);
        }

        Calendar result = null;
        if (date != null) {
            result = today();
            result.setTime(date);
        }

        return result;
    }

    public void setStartDate(Calendar startDate) {
        if (startDate == null) {
            _schedule.startDate = null;
        } else {
            _schedule.startDate = _dateFormatYMD.format(startDate.getTime());
            Log.d(LOG_TAG, "schedule.startDate: " + _schedule.startDate);
        }
    }

    public Calendar getStartDate() {
        Date date = null;
        try {
            date = _dateFormatYMD.parse(_schedule.startDate);
        } catch (ParseException e) {
            Log.d(LOG_TAG, "Invalid date string (startDate): " + _schedule.startDate);
        }

        Calendar result = null;
        if (date != null) {
            result = today();
            result.setTime(date);
        }

        return result;
    }

    public void setEndDate(Calendar endDate) {
        if (endDate == null) {
            _schedule.endDate = null;
        } else {
            _schedule.endDate = _dateFormatYMD.format(endDate.getTime());
            Log.d(LOG_TAG, "schedule.endDate: " + _schedule.startDate);
        }
    }

    public Calendar getEndDate() {
        Date date = null;
        if (_schedule.endDate != null) {
            try {
                date = _dateFormatYMD.parse(_schedule.endDate);
            } catch (ParseException e) {
                Log.d(LOG_TAG, "Invalid date string (endDate): " + _schedule.endDate);
            }
        }

        Calendar result = null;
        if (date != null) {
            result = today();
            result.setTime(date);
        }

        return result;
    }

    public void setDuration(int duration) {
        _schedule.duration = duration;
    }

    public int getDuration() {
        return _schedule.duration;
    }

    public void setInterval(int interval) {
        _schedule.interval = interval;
    }

    public int getInterval() {
        return _schedule.interval;
    }

    public void setDaysOfWeek(Set<Integer> daysOfWeek) {
        if (daysOfWeek == null) {
            _schedule.daysOfWeek = new int[0];
        } else {
            _schedule.daysOfWeek = new int[daysOfWeek.size()];
            int i = 0;
            for (Integer day : daysOfWeek) {
                _schedule.daysOfWeek[i++] = day;
            }
        }
    }

    public Set<Integer> getDaysOfWeek() {
        if (_schedule.daysOfWeek == null) {
            return new HashSet<>();
        }

        Set<Integer> result = new HashSet(_schedule.daysOfWeek.length);
        for (int i = 0; i < _schedule.daysOfWeek.length; i++) {
            result.add(_schedule.daysOfWeek[i]);
        }

        return result;
    }

    public void setDaysOfMonth(Set<Integer> daysOfMonth) {
        if (daysOfMonth == null) {
            _schedule.daysOfMonth = new int[0];
        } else {
            _schedule.daysOfMonth = new int[daysOfMonth.size()];
            int i = 0;
            for (Integer day : daysOfMonth) {
                _schedule.daysOfMonth[i++] = day;
            }
        }
    }

    public Set<Integer> getDaysOfMonth() {
        if (_schedule.daysOfMonth == null) {
            return new HashSet<>();
        }

        Set<Integer> result = new HashSet<Integer>(_schedule.daysOfMonth.length);
        for (int i = 0; i < _schedule.daysOfMonth.length; i++) {
            result.add(_schedule.daysOfMonth[i]);
        }

        return result;
    }

    public void setMonthsOfYear(Set<Integer> monthsOfYear) {
        if (monthsOfYear == null) {
            _schedule.monthsOfYear = new int[0];
        } else {
            _schedule.monthsOfYear = new int[monthsOfYear.size()];
            int i = 0;
            for (Integer day : monthsOfYear) {
                _schedule.monthsOfYear[i++] = day;
            }
        }
    }

    public Set<Integer> getMonthsOfYear() {
        if (_schedule.monthsOfYear == null) {
            return new HashSet<>();
        }

        Set<Integer> result = new HashSet<Integer>(_schedule.monthsOfYear.length);
        for (int i = 0; i < _schedule.monthsOfYear.length; i++) {
            result.add(_schedule.monthsOfYear[i]);
        }

        return result;
    }

    public void setDayOccurOfMonth(Set<Integer> dayOccurOfMonth) {
        if (dayOccurOfMonth == null) {
            _schedule.dayOccurOfMonth = new int[0];
        } else {
            _schedule.dayOccurOfMonth = new int[dayOccurOfMonth.size()];
            int i = 0;
            for (Integer day : dayOccurOfMonth) {
                _schedule.dayOccurOfMonth[i++] = day;
            }
        }
    }

    public Set<Integer> getDayOccurOfMonth() {
        if (_schedule.dayOccurOfMonth == null) {
            return new HashSet<>();
        }

        Set<Integer> result = new HashSet<Integer>(_schedule.dayOccurOfMonth.length);
        for (int i = 0; i < _schedule.dayOccurOfMonth.length; i++) {
            result.add(_schedule.dayOccurOfMonth[i]);
        }

        return result;
    }

    /**
     * Helper method to return the on time as reflected in the schedule
     *
     * @return The on time for the schedule, or null if no on time is set
     */
    @Nullable
    public Calendar getOnTime() {
        boolean offTimeAtEnd = true;
        if (_schedule.scheduleActions != null && _schedule.scheduleActions.length > 0) {
            AylaScheduleAction action = _schedule.scheduleActions[0];
            if (action.active && (action.atEnd && action.value.equals("1")) ||
                    (action.atStart && action.value.equals("0"))) {
                offTimeAtEnd = false;
            }
        }

        String dateString;
        if (offTimeAtEnd) {
            dateString = _schedule.startTimeEachDay;
        } else {
            dateString = _schedule.endTimeEachDay;
        }

        Date date = null;
        try {
            date = _dateFormatHMS.parse(dateString);
        } catch (ParseException e) {
            Log.e(LOG_TAG, "Failed to parse date: " + dateString);
            e.printStackTrace();
            return null;
        }

        Calendar calendar = today();
        calendar.setTime(date);
        return calendar;
    }

    /**
     * Helper method to return the off time of the schedule.
     *
     * @return The off time, or null if no off time is set
     */
    @Nullable
    public Calendar getOffTime() {
        boolean offTimeAtEnd = true;
        if (_schedule.scheduleActions != null && _schedule.scheduleActions.length > 0) {
            AylaScheduleAction action = _schedule.scheduleActions[0];
            if (action.active && (action.atEnd && action.value.equals("1")) ||
                    (action.atStart && action.value.equals("0"))) {
                offTimeAtEnd = false;
            }
        }

        String dateString;
        if (offTimeAtEnd) {
            dateString = _schedule.endTimeEachDay;
        } else {
            dateString = _schedule.startTimeEachDay;
        }

        Date date;
        try {
            date = _dateFormatHMS.parse(dateString);
        } catch (ParseException e) {
            Log.e(LOG_TAG, "Failed to parse date: " + dateString);
            e.printStackTrace();
            return null;
        }

        Calendar calendar = today();
        calendar.setTime(date);
        return calendar;
    }

    /**
     * Helper method to set the on time for a schedule. This method will set the appropriate
     * schedule actions for the event, taking the offTime into consideration if necessary
     *
     * @param onTime time to turn the device on
     */
    public void setOnTime(Calendar onTime) {
        Calendar offTime = getOffTime();
        if (onTime == null) {
            // If we have an off time set, make sure we have exactly one action set to
            // turn off the device at the startTime.
            if (offTime == null) {
                // Deactivate all actions
                for (AylaScheduleAction action : _schedule.scheduleActions) {
                    action.active = false;
                }
            } else {
                // Set the off time as the schedule start time, and have one action to turn
                // the device off
                setStartTimeEachDay(offTime);
                for ( String propertyName : _actionProperties ) {
                    List<AylaScheduleAction> actions = getActionsForProperty(propertyName);
                    AylaScheduleAction action = actions.get(0);
                    action.atStart = true;
                    action.atEnd = false;
                    action.value = "0";
                    action.active = true;
                }
            }
        } else {
            if (offTime != null) {
                for ( String propertyName : _actionProperties ) {
                    List<AylaScheduleAction> actions = getActionsForProperty(propertyName);
                    if ( actions.size() != 2 ) {
                        Log.e(LOG_TAG, "Invalid number of actions (" + actions.size() +
                                ") for property " + propertyName);
                        continue;
                    }

                    AylaScheduleAction action1 = actions.get(0);
                    AylaScheduleAction action2 = actions.get(1);

                    // See who comes first
                    if (onTime.compareTo(offTime) < 0) {
                        // onTime comes before offTime
                        setStartTimeEachDay(onTime);
                        setEndDate(offTime);
                        action1.value = "1";
                        action1.atStart = true;
                        action1.atEnd = false;
                        action1.active = true;

                        action2.value = "0";
                        action2.atStart = false;
                        action2.atEnd = true;
                        action2.active = true;
                    } else {
                        // onTime comes after offTime
                        setStartTimeEachDay(offTime);
                        setEndTimeEachDay(onTime);
                        action1.value = "0";
                        action1.atStart = true;
                        action1.atEnd = false;
                        action1.active = true;

                        action2.value = "1";
                        action2.atStart = false;
                        action2.atEnd = true;
                        action2.active = true;
                    }
                }
            } else {
                // No off time set. We have one action only
                setStartTimeEachDay(onTime);
                for ( String propertyName : _actionProperties ) {
                    List<AylaScheduleAction> actions = getActionsForProperty(propertyName);
                    if ( actions.size() != 2 ) {
                        Log.e(LOG_TAG, "Invalid number of actions (" + actions.size() +
                                ") for property " + propertyName);
                        continue;
                    }

                    AylaScheduleAction action1 = actions.get(0);
                    AylaScheduleAction action2 = actions.get(1);
                    action1.value = "1";
                    action1.atStart = true;
                    action1.atEnd = false;
                    action1.active = true;

                    action2.active = false;
                }
            }
        }
    }

    /**
     * Helper method to set the off time for a schedule. This method will set the appropriate
     * schedule actions for the event, taking the onTime into consideration if necessary
     *
     * @param offTime time to turn the device off
     */
    public void setOffTime(Calendar offTime) {
        Calendar onTime = getOnTime();

        if (offTime == null) {
            // If we have an on time set, make sure we have exactly one action set to
            // turn off the device at the startTime.
            if (onTime == null) {
                // Remove all actions
                for (AylaScheduleAction action : _schedule.scheduleActions) {
                    action.active = false;
                }
            } else {
                // Set the on time as the schedule start time, and have one action to turn
                // the device on
                setStartTimeEachDay(onTime);
                AylaScheduleAction action = _schedule.scheduleActions[0];
                action.atStart = true;
                action.atEnd = false;
                action.value = "1";
                action.active = true;
                _schedule.scheduleActions[1].active = false;
            }
        } else {
            if (onTime != null) {
                // There is an on time as well. Set up the actions appropriately
                AylaScheduleAction action1 = _schedule.scheduleActions[0];
                AylaScheduleAction action2 = _schedule.scheduleActions[1];

                // See who comes first
                if (offTime.compareTo(onTime) < 0) {
                    // offTime comes before onTime
                    setStartTimeEachDay(offTime);
                    setEndTimeEachDay(onTime);
                    action1.value = "0";
                    action1.atStart = true;
                    action1.atEnd = false;
                    action1.active = true;

                    action2.value = "1";
                    action2.atStart = false;
                    action2.atEnd = true;
                    action2.active = true;
                } else {
                    // offTime comes after onTime
                    setStartTimeEachDay(onTime);
                    setEndTimeEachDay(offTime);

                    action1.value = "1";
                    action1.atStart = true;
                    action1.atEnd = false;
                    action1.active = true;

                    action2.value = "0";
                    action2.atStart = false;
                    action2.atEnd = true;
                    action2.active = true;
                }
            } else {
                // No on time set. We have one action only
                setStartTimeEachDay(onTime);
                _schedule.scheduleActions[0].value = "0";
                _schedule.scheduleActions[0].atStart = true;
                _schedule.scheduleActions[0].atEnd = false;
                _schedule.scheduleActions[0].active = true;

                _schedule.scheduleActions[1].active = false;
            }
        }
    }

    public void setIsTimer(boolean isTimer) {
        if (isTimer) {
            setEndDate(today());
        } else {
            setEndDate(null);
        }
    }

    public boolean isTimer() {
        return getEndDate() != null;
    }

    public AylaSchedule getSchedule() {
        return _schedule;
    }

    public void updateScheduleActions() {
        // Update our schedule actions array based on our actions and dates
        Calendar oldOnTime = getOnTime();
        Calendar oldOffTime = getOffTime();

        boolean startEndReversed = (isTimer() &&
                oldOnTime != null &&
                oldOffTime != null &&
                (oldOnTime.compareTo(oldOffTime) < 0));

        // Are we setting any actions at all?
        if (_actionProperties.size() == 0) {
            // No actions to set.
            for (AylaScheduleAction action : _schedule.scheduleActions) {
                action.active = false;
            }
            return;
        }

        // First disable all actions
        for ( AylaScheduleAction action : _schedule.scheduleActions ) {
            action.active = false;
        }

        // Set the start / end actions for each property
        for (String propertyName : _actionProperties ) {
            List<AylaScheduleAction> actions = getActionsForProperty(propertyName);
            if ( actions.size() != 2 ) {
                Log.d(LOG_TAG, "Wrong number of actions (" + actions.size() + ") set up for " + propertyName);
                continue;
            }

            // The "on" action
            AylaScheduleAction action = actions.get(0);
            action.active = true;
            action.atStart = !startEndReversed;
            action.atEnd = startEndReversed;
            action.value = "1";

            // The "off" action
            action = actions.get(1);
            action.active = true;
            action.atStart = startEndReversed;
            action.atEnd = !startEndReversed;
            action.value = "0";
        }
    }

    private List<AylaScheduleAction> getActionsForProperty(String propertyName) {
        List<AylaScheduleAction> actions = new ArrayList<>();
        for ( AylaScheduleAction action : _schedule.scheduleActions ) {
            if ( action.name.equals(propertyName)) {
                actions.add(action);
            }
        }

        return actions;
    }

    protected Calendar today() {
        Calendar cal = Calendar.getInstance();
        return cal;
    }

    @Override
    public String toString() {
        return _schedule.name + (_schedule.active ? " (active)" : " (not active)");
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Private Section
    //////////////////////////////////////////////////////////////////////////////////////////

    // Static date formatters
    private static DateFormat _dateFormatHMS;
    private static DateFormat _dateFormatYMD;

    /**
     * Static initialization
     */
    static {
        _dateFormatHMS = new SimpleDateFormat("HH:mm:ss");
        _dateFormatHMS.setTimeZone(TimeZone.getTimeZone("UTC"));

        _dateFormatYMD = new SimpleDateFormat("yyyy-MM-dd");
        _dateFormatYMD.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
 }
