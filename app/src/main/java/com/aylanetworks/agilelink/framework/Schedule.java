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
import java.util.Locale;
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
 * <p>
 * This class supports two schedule "types", a recurring schedule and a timer. The recurring
 * schedule does not expire, and
 */
public class Schedule implements  Cloneable {
    private static String LOG_TAG = "Schedule";

    // The internal AylaSchedule object
    private AylaSchedule _schedule;

    // Maps of property names to their enable / disable action strings
    private Set<String> _actionProperties;

    // Time zone for the device this schedule is for
    private TimeZone _timeZone;

    // Date formatters. We use formatters in the time zone of the device for schedules,
    // and a static formatter in UTC for timers.
    private DateFormat _dateFormatHMS;
    private DateFormat _dateFormatYMD;

    static DateFormat _dateFormatHMSUTC;
    static {
        _dateFormatHMSUTC = new SimpleDateFormat("HH:mm:ss", Locale.US);
        _dateFormatHMSUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Creates a Schedule object initialized with the given AylaSchedule object
     *
     * @param schedule AylaSchedule used to initialize this Schedule object
     */
    public Schedule(AylaSchedule schedule, TimeZone timeZone) {
        _schedule = schedule;

        if ( timeZone == null ) {
            _timeZone = TimeZone.getTimeZone("UTC");
            _schedule.utc = true;
        } else {
            _timeZone = timeZone;
            _schedule.utc = false;
        }

        _actionProperties = new HashSet<>();

        _dateFormatHMS = new SimpleDateFormat("HH:mm:ss");
        _dateFormatHMS.setTimeZone(_timeZone);

        _dateFormatYMD = new SimpleDateFormat("yyyy-MM-dd");
        _dateFormatYMD.setTimeZone(_timeZone);
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

    public Set<String> getActions() {
        return new HashSet<>(_actionProperties);
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
     * Clears the schedule by setting the start date to null and setting fields to default values
     */
    public void clearSchedule() {
        _schedule.startDate = null;
        _schedule.endDate = null;
        _schedule.daysOfWeek = new int[]{1, 2, 3, 4, 5, 6, 7};
        _schedule.startTimeEachDay = null;
        _schedule.endTimeEachDay = null;
        _schedule.duration = 0;
    }

    /**
     * Returns true if the schedule is clear. This means that the schedule has not been configured
     * by the user, but rather is set to default values.
     * @return true if the schedule is clear, false if the schedule has been configured
     */
    public boolean isScheduleClear() {
        return _schedule.startDate == null;
    }

    /** Returns a copy of this schedule object. This copy is a deep copy.
     *
     * @return A deep copy of this object
     */
    @Override
    public Schedule clone() throws CloneNotSupportedException {
        super.clone();
        Schedule other = new Schedule(_schedule, _timeZone);
        return cloneTo(other);
    }

    /**
     * Copies this schedule object to another existing schedule object.
     * @param other Schedule object to copy this object onto
     * @return The other schedule object with its values copied from ours
     */
    public Schedule cloneTo(Schedule other) {
        other._schedule.startDate = _schedule.startDate;
        other._schedule.direction = _schedule.direction;
        other._schedule.name = _schedule.name;
        other._schedule.displayName = _schedule.displayName;
        other._schedule.active = _schedule.active;
        other._schedule.utc = _schedule.utc;
        other._schedule.endDate = _schedule.endDate;
        other._schedule.startTimeEachDay = _schedule.startTimeEachDay;
        other._schedule.endTimeEachDay = _schedule.endTimeEachDay;
        if ( _schedule.daysOfWeek != null )
            other._schedule.daysOfWeek = _schedule.daysOfWeek.clone();
        if ( _schedule.daysOfMonth != null )
            other._schedule.daysOfMonth = _schedule.daysOfMonth.clone();
        if ( _schedule.monthsOfYear != null )
            other._schedule.monthsOfYear = _schedule.monthsOfYear.clone();
        if ( _schedule.dayOccurOfMonth != null )
            other._schedule.dayOccurOfMonth = _schedule.dayOccurOfMonth.clone();
        other._schedule.duration = _schedule.duration;
        other._schedule.interval = _schedule.interval;
        if ( _schedule.scheduleActions != null )
            other._schedule.scheduleActions = _schedule.scheduleActions.clone();

        return other;
    }

    /**
     * Updates the time zone used for this schedule. Will update the start / end times to reflect
     * the new time zone.
     * @param timeZone Time zone to use for this schedule. Pass null to use UTC time.
     */
    public void setTimeZone(TimeZone timeZone) {
        // Save the previous date formatter so we can update the times propertly
        DateFormat oldFormat = _dateFormatHMS;

        if ( timeZone == null || timeZone.equals(TimeZone.getTimeZone("UTC"))) {
            _schedule.utc = true;
            _timeZone = TimeZone.getTimeZone("UTC");
            _dateFormatHMS.setTimeZone(_timeZone);
        } else {
            _schedule.utc = false;
            _timeZone = timeZone;
            _dateFormatHMS.setTimeZone(timeZone);
        }
    }

    public TimeZone getTimeZone() {
        return _timeZone;
    }

    /**
     * Sets the schedules start time each day
     *
     * @param startTime Time the schedule should start each day
     */
    public void setStartTimeEachDay(Calendar startTime) {
        _schedule.utc = false;
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
        } catch (ParseException | NullPointerException e) {
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
        _schedule.utc = false;
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
        } catch (ParseException | NullPointerException e) {
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
        } catch (ParseException | NullPointerException e) {
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
            Log.d(LOG_TAG, "schedule.endDate: " + _schedule.endDate);
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

    public void setAllDaysOfWeek() {
        _schedule.daysOfWeek = new int[]{1,2,3,4,5,6,7};
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
    public Calendar getTimerOnTime() {
        boolean offTimeAtEnd = true;
        if (_schedule.scheduleActions != null && _schedule.scheduleActions.length > 0) {
            AylaScheduleAction action = _schedule.scheduleActions[0];
            if (action.active && (action.atEnd && action.value.equals("1")) ||
                    (action.atStart && action.value.equals("0"))) {
                offTimeAtEnd = false;
            }
        }

        Date date = null;
        try {
            date = _dateFormatHMSUTC.parse(_schedule.startTimeEachDay);
        } catch (ParseException | NullPointerException e) {
            Log.e(LOG_TAG, "Failed to parse date: " + _schedule.startTimeEachDay);
            e.printStackTrace();
            return null;
        }

        Calendar calendar = today();
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        calendar.setTime(date);
        if ( !offTimeAtEnd ) {
            calendar.add(Calendar.SECOND, _schedule.duration);
        }
        return calendar;
    }

    /**
     * Helper method to return the off time of the schedule.
     *
     * @return The off time, or null if no off time is set
     */
    @Nullable
    public Calendar getTimerOffTime() {
        boolean offTimeAtEnd = true;
        if (_schedule.scheduleActions != null && _schedule.scheduleActions.length > 0) {
            AylaScheduleAction action = _schedule.scheduleActions[0];
            if (action.active && (action.atEnd && action.value.equals("1")) ||
                    (action.atStart && action.value.equals("0"))) {
                offTimeAtEnd = false;
            }
        }

        Date date;
        try {
            date = _dateFormatHMSUTC.parse(_schedule.startTimeEachDay);
        } catch (ParseException | NullPointerException e) {
            Log.e(LOG_TAG, "Failed to parse date: " + _schedule.startTimeEachDay);
            e.printStackTrace();
            return null;
        }

        Calendar calendar = today();
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        calendar.setTime(date);
        if ( offTimeAtEnd ) {
            calendar.add(Calendar.SECOND, _schedule.duration);
        }
        return calendar;
    }


    /**
     * Helper method to set the times for a schedule. This method will set the appropriate
     * schedule actions for the event.
     *
     * @param onMinutes Number of minutes from now to turn on the device
     * @param offMinutes Number of minutes from now to turn off the device
     */
    public void setTimer(int onMinutes, int offMinutes) {
        _schedule.utc = true;
        _schedule.endTimeEachDay = "";
        setAllDaysOfWeek();

        Calendar scheduleStartTime = Calendar.getInstance();

        boolean onAtStart = true;
        int duration = Math.abs(onMinutes - offMinutes) * 60;

        if ( onMinutes > offMinutes ) {
            // We turn off first. That will be the schedule start.
            scheduleStartTime.add(Calendar.MINUTE, offMinutes);
            onAtStart = false;
        } else {
            // We turn on first. That will be the schedule start.
            scheduleStartTime.add(Calendar.MINUTE, onMinutes);
        }

        _schedule.startDate = _dateFormatYMD.format(scheduleStartTime.getTime());
        _schedule.startTimeEachDay = _dateFormatHMSUTC.format(scheduleStartTime.getTime());
        _schedule.duration = duration;

        scheduleStartTime.add(Calendar.SECOND, duration);
        _schedule.endDate = _dateFormatYMD.format(scheduleStartTime.getTime());

        AylaScheduleAction action1 = _schedule.scheduleActions[0];
        AylaScheduleAction action2 = _schedule.scheduleActions[1];

        // Turn off action
        action1.value = "0";
        action1.atStart = !onAtStart;
        action1.atEnd = onAtStart;
        action1.active = (offMinutes != 0);

        // Turn on action
        action2.value = "1";
        action2.atStart = onAtStart;
        action2.atEnd = !onAtStart;
        action2.active = (onMinutes != 0);
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
        Calendar oldOnTime = getTimerOnTime();
        Calendar oldOffTime = getTimerOffTime();

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
 }
