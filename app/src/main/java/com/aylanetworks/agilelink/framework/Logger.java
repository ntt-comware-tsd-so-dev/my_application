package com.aylanetworks.agilelink.framework;

import android.os.Message;
import android.text.TextUtils;

import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.agilelink.BuildConfig;

/*
 * Logger.java
 * AgileLink Application Framework
 *
 * Created by David Junod on 5/18/2015.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */
public class Logger {

    public static void logMessage(String tag, String text, Message msg) {
        String jsonResult = (String)msg.obj;
        if (!TextUtils.isEmpty(jsonResult)) {
            jsonResult = jsonResult.replace("\n", "");
        }
        if (AylaNetworks.succeeded(msg)) {
            intLogMessage(LogLevel.Info, tag, "results:[%s], %s", jsonResult, text);
        } else {
            intLogMessage(LogLevel.Error, tag, "results:%d:%d:[%s], %s", msg.what, msg.arg1, jsonResult, text);
        }
    }

    public static void logError(String tag, String msg) {
        intLogMessage(LogLevel.Error, tag, msg);
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
    }
}
