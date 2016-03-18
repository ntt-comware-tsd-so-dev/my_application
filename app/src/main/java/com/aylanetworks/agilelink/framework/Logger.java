package com.aylanetworks.agilelink.framework;

import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.agilelink.BuildConfig;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/*
 * Logger.java
 * AgileLink Application Framework
 *
 * Created by David Junod on 5/18/2015.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */
public class Logger implements Thread.UncaughtExceptionHandler {

    private final static String LOG_TAG = "Logger";

    /**
     * The LoggerListener interface enables application developers
     * to extend the Logger for such purposes as Google Analytics
     * or any other logging services.
     */
    public interface LoggerListener {

        /**
         * Write an entry to the log
         * @param level The LogLevel of the log entry
         * @param tag Module tag of the log entry
         * @param msg The detail message of the log entry.
         */
        void loggerWriteLogMessage(LogLevel level, String tag, String msg);

        /**
         * Uncaught exception handler
         * @param thread The thread where the exception occurred
         * @param t The exception
         */
        void loggerUncaughtException(Thread thread, Throwable t);
    }

    /**
     * The logger singleton object
     */
    private static Logger sInstance;

    /**
     * Returns the singleton instance of the Logger
     *
     * @return the Logger object
     */
    public static Logger getInstance() {
        if (sInstance == null) {
            sInstance = new Logger();
        }
        return sInstance;
    }

    /**
     * Initialize the Logger. Must be called from the Application object's OnCreate
     * method.
     */
    public void initialize() {
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    /**
     * The default system handler for Uncaught Exceptions
     */
    private Thread.UncaughtExceptionHandler mDefaultHandler;

    @Override
    public void uncaughtException(Thread thread, Throwable t) {
        Log.e(LOG_TAG, LOG_TAG + ": ### uncaughtException : " + t.getMessage());
        mDefaultHandler.uncaughtException(thread, t);
        sInstance.notifyUncaughtException(thread, t);
        intLogMessage(LogLevel.CriticalTerminating, LOG_TAG, throwableToString(t));
    }

    /**
     * Convert a Throwable to String for output to the log
     * @param t Throwable (Exception)
     * @return String describing the exception
     */
    public static String throwableToString(Throwable t) {
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        t.printStackTrace(printWriter);
        String stacktrace = result.toString();
        printWriter.close();
        return stacktrace;
    }

    /**
     * The set of Logger listeners
     */
    private Set<LoggerListener> mListeners = null;

    /**
     * Add a LoggerListener
     * @param l The LoggerListener to add.
     */
    public void addListener(LoggerListener l) {
        getListeners().add(l);
    }

    /**
     * Remove a LoggerListener
     * @param l The LoggerListener to remove.
     */
    public void removeListener(LoggerListener l) {
        getListeners().remove(l);
    }

    /**
     * Get an enumerator of the LoggerListener
     * @return A Set containing all the LoggerListener
     */
    public Set<LoggerListener> getListeners() {
        synchronized (this) {
            if (mListeners == null) {
                mListeners = new CopyOnWriteArraySet<LoggerListener>();
            }
            return mListeners;
        }
    }

    private void notifyWriteLogMessage(LogLevel level, String tag, String msg) {
        if (mListeners != null) {
            for (LoggerListener l : getListeners()) {
                l.loggerWriteLogMessage(level, tag, msg);
            }
        }
    }

    private void notifyUncaughtException(Thread thread, Throwable t) {
        if (mListeners != null) {
            for (LoggerListener l : getListeners()) {
                l.loggerUncaughtException(thread, t);
            }
        }
    }

    /**
     * Helper method for logging information about an Ayla Message
     * @param tag Module tag of the log entry
     * @param msg The Ayla Message to log information about.
     * @param text Detail text for the log entry
     */
    /**
     * Helper method for logging information about an Ayla Message
     * @param tag Module tag of the log entry
     * @param msg The Ayla Message to log information about.
     * @param fmt  Detail text for the log entry supporting formatting
     * @param args Optional arguments.
     */
    public static void logMessage(String tag, Message msg, String fmt, Object... args) {
        String detail = String.format(fmt, args);
        String jsonResult = "";
        if (msg.obj != null) {
            if (msg.obj instanceof String) {
                jsonResult = (String) msg.obj;
            } else {
                jsonResult = msg.obj.getClass().getSimpleName();
            }
        }
        if (!TextUtils.isEmpty(jsonResult)) {
            jsonResult = jsonResult.replace("\n", "");
        }
        if (AylaNetworks.succeeded(msg)) {
            intLogMessage(LogLevel.Info, tag, "%s results:[%s]", detail, jsonResult);
        } else {
            intLogMessage(LogLevel.Error, tag, "%s results:%d:%d:[%s]", detail, msg.what, msg.arg1, jsonResult);
        }
    }

    /**
     * Write an error to the log
     * @param tag Module tag of the log entry
     * @param msg Detail text for the log entry
     */
    public static void logError(String tag, String msg) {
        intLogMessage(LogLevel.Error, tag, msg);
    }

    public static void logError(String tag, Exception ex) {
        intLogMessage(LogLevel.Error, tag, ex.getLocalizedMessage());
    }

    public static void logError(String tag, Exception ex, String fmt, Object... args) {
        String detail = String.format(fmt, args);
        intLogMessage(LogLevel.Error, tag, String.format("%s '%s'", detail, ex.getLocalizedMessage()));
    }

    public static void logError(String tag, String fmt, Object... args) {
        intLogMessage(LogLevel.Error, tag, String.format(fmt, args));
    }

    public static void logWarning(String tag, String msg) {
        intLogMessage(LogLevel.Warning, tag, msg);
    }

    public static void logWarning(String tag, String fmt, Object... args) {
        intLogMessage(LogLevel.Warning, tag, String.format(fmt, args));
    }

    public static void logInfo(String tag, String msg) {
        intLogMessage(LogLevel.Info, tag, msg);
    }

    public static void logInfo(String tag, String fmt, Object... args) {
        intLogMessage(LogLevel.Info, tag, String.format(fmt, args));
    }

    public static void logVerbose(String tag, String msg) {
        intLogMessage(LogLevel.Verbose, tag, msg);
    }

    public static void logVerbose(String tag, String fmt, Object... args) {
        intLogMessage(LogLevel.Verbose, tag, String.format(fmt, args));
    }

    public static void logDebug(String tag, String msg) {
        intLogMessage(LogLevel.Debug, tag, msg);
    }

    public static void logDebug(String tag, String fmt, Object... args) {
        intLogMessage(LogLevel.Debug, tag, String.format(fmt, args));
    }

    public static void logPass(String tag, String msg) {
        intLogMessage(LogLevel.Pass, tag, msg);
    }

    public static void logPass(String tag, String fmt, Object... args) {
        intLogMessage(LogLevel.Pass, tag, String.format(fmt, args));
    }

    public static void logFail(String tag, String msg) {
        intLogMessage(LogLevel.Fail, tag, msg);
    }

    public static void logFail(String tag, String fmt, Object... args) {
        intLogMessage(LogLevel.Fail, tag, String.format(fmt, args));
    }

    /**
     * enum of the log levels available.
     */
    public enum LogLevel {
        CriticalTerminating,
        Critical,
        Error,
        Warning,
        Fail,
        Pass,
        Info,
        Verbose,
        Debug,
    }

    private static void intLogMessage(LogLevel level, String tag, String fmt, Object... args) {
        intLogMessage(level, tag, String.format(fmt, args));
    }

    private static void intLogMessage(LogLevel level, String tag, String msg) {
        String l = "?";
        switch (level) {
            case CriticalTerminating:
                l = "T";
                break;
            case Critical:
                l = "C";
                break;
            case Error:
                l = "E";
                break;
            case Warning:
                l = "W";
                break;
            case Fail:
                l = "F";
                break;
            case Pass:
                l = "P";
                break;
            case Info:
                l = "I";
                break;
            case Verbose:
                if (!BuildConfig.DEBUG)
                    return;
                l = "V";
                break;
            case Debug:
                if (!BuildConfig.DEBUG)
                    return;
                l = "D";
                break;
        }
        AylaSystemUtils.saveToLog("%s, %s, %s", l, tag, msg);
        sInstance.notifyWriteLogMessage(level, tag, msg);
    }
}