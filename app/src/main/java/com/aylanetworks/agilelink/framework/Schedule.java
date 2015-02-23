package com.aylanetworks.agilelink.framework;

import android.util.Log;

import com.aylanetworks.aaml.AylaSchedule;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Brian King on 2/23/15.
 */

/**
 * Class representing a Schedule for a device. This class wraps the AylaSchedule object and provides
 * convenience methods to manipulate, save and fetch schedule information for a device.
 */
public class Schedule {
    private static String LOG_TAG = "Schedule";

    // The internal AylaSchedule object
    AylaSchedule _schedule;

    /**
     * Creates a blank Schedule object
     */
    public Schedule() {
        _schedule = new AylaSchedule();
    }

    /**
     * Creates a Schedule object initialized with the given AylaSchedule object
     * @param schedule AylaSchedule used to initialize this Schedule object
     */
    public Schedule(AylaSchedule schedule) {
        _schedule = schedule;
    }

    /**
     * Sets the name of this schedule
     * @param name The name for the schedule
     */
    public void setName(String name) {
        _schedule.name = name;
    }

    /**
     * Returns the name of the schedule
     * @return the name of the schedule
     */
    public String getName() {
        return _schedule.name;
    }

    /**
     * Sets the schedule to be active or inactive
     * @param active true to activate, false to deactivate
     */
    public void setActive(boolean active) {
        _schedule.active = active;
    }

    /**
     * Returns true if the schedule is active
     * @return true if active, false if inactive
     */
    public boolean isActive() {
        return _schedule.active;
    }

    /**
     * Sets the schedules start time each day
     * @param startTime Time the schedule should start each day
     */
    public void setStartTimeEachDay(Calendar startTime) {
        if ( startTime == null ) {
            _schedule.startTimeEachDay = null;
        } else {
            _schedule.startTimeEachDay = _dateFormatHMS.format(startTime.getTime());
        }
    }

    /**
     * Returns the schedule's start time each day
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
        if ( date != null ) {
            result = Calendar.getInstance();
            result.setTime(date);
        }

        return result;
    }

    public void setEndTimeEachDay(Calendar endTime) {
        if ( endTime == null ) {
            _schedule.endTimeEachDay = null;
        } else {
            _schedule.endTimeEachDay = _dateFormatHMS.format(endTime.getTime());
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
        if ( date != null ) {
            result = Calendar.getInstance();
            result.setTime(date);
        }

        return result;
   }

    public void setStartDate(Calendar startDate) {
        if ( startDate == null ) {
            _schedule.startDate = null;
        } else {
            _schedule.startDate = _dateFormatYMD.format(startDate.getTime());
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
        if ( date != null ) {
            result = Calendar.getInstance();
            result.setTime(date);
        }

        return result;
    }

    public void setEndDate(Calendar endDate) {
        if ( endDate == null ) {
            _schedule.endDate = null;
        } else {
            _schedule.endDate = _dateFormatYMD.format(endDate.getTime());
        }
    }

    public Calendar getEndDate() {
        Date date = null;
        try {
            date = _dateFormatYMD.parse(_schedule.endDate);
        } catch (ParseException e) {
            Log.d(LOG_TAG, "Invalid date string (endDate): " + _schedule.endDate);
        }

        Calendar result = null;
        if ( date != null ) {
            result = Calendar.getInstance();
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
        if ( daysOfWeek == null ) {
            _schedule.daysOfWeek = new int[0];
        } else {
            _schedule.daysOfWeek = new int[daysOfWeek.size()];
            int i = 0;
            for ( Integer day : daysOfWeek ) {
                _schedule.daysOfWeek[i++] = day;
            }
        }
    }

    public Set<Integer>getDaysOfWeek() {
        if ( _schedule.daysOfWeek == null ) {
            return new HashSet<>();
        }

        Set<Integer> result = new HashSet(_schedule.daysOfWeek.length);
        for ( int i = 0; i < _schedule.daysOfWeek.length; i++ ) {
            result.add(_schedule.daysOfWeek[i]);
        }

        return result;
    }

    public void setDaysOfMonth(Set<Integer> daysOfMonth) {
        if ( daysOfMonth == null ) {
            _schedule.daysOfMonth = new int[0];
        } else {
            _schedule.daysOfMonth = new int[daysOfMonth.size()];
            int i = 0;
            for ( Integer day : daysOfMonth ) {
                _schedule.daysOfMonth[i++] = day;
            }
        }
    }

    public Set<Integer>getDaysOfMonth() {
        if ( _schedule.daysOfMonth == null ) {
            return new HashSet<>();
        }

        Set<Integer> result = new HashSet(_schedule.daysOfMonth.length);
        for ( int i = 0; i < _schedule.daysOfMonth.length; i++ ) {
            result.add(_schedule.daysOfMonth[i]);
        }

        return result;
    }

    public void setMonthsOfYear(Set<Integer> monthsOfYear) {
        if ( monthsOfYear == null ) {
            _schedule.monthsOfYear = new int[0];
        } else {
            _schedule.monthsOfYear = new int[monthsOfYear.size()];
            int i = 0;
            for ( Integer day : monthsOfYear ) {
                _schedule.monthsOfYear[i++] = day;
            }
        }
    }

    public Set<Integer>getMonthsOfYear() {
        if ( _schedule.monthsOfYear == null ) {
            return new HashSet<>();
        }

        Set<Integer> result = new HashSet(_schedule.monthsOfYear.length);
        for ( int i = 0; i < _schedule.monthsOfYear.length; i++ ) {
            result.add(_schedule.monthsOfYear[i]);
        }

        return result;
    }

    public void setDayOccurOfMonth(Set<Integer> dayOccurOfMonth) {
        if ( dayOccurOfMonth == null ) {
            _schedule.dayOccurOfMonth = new int[0];
        } else {
            _schedule.dayOccurOfMonth = new int[dayOccurOfMonth.size()];
            int i = 0;
            for ( Integer day : dayOccurOfMonth ) {
                _schedule.dayOccurOfMonth[i++] = day;
            }
        }
    }

    public Set<Integer>getDayOccurOfMonth() {
        if ( _schedule.dayOccurOfMonth == null ) {
            return new HashSet<>();
        }

        Set<Integer> result = new HashSet<Integer>(_schedule.dayOccurOfMonth.length);
        for ( int i = 0; i < _schedule.dayOccurOfMonth.length; i++ ) {
            result.add(_schedule.dayOccurOfMonth[i]);
        }

        return result;
    }

    public AylaSchedule getSchedule() {
        return _schedule;
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
        _dateFormatYMD = new SimpleDateFormat("yyyy-MM-dd");
    }
}
