package com.aylanetworks.agilelink;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ServerError;

/**
 * Created by Kevin Liu on 6/14/16.
 */
public class ErrorUtils {

    private final static int SERVER_ERROR_TOKEN_EXPIRE = 403;
    private final static int SERVER_ERROR_NOT_FOUND = 404; // No results were found
    private final static int SERVER_ERROR_TIMEOUT = 408; // Request timeout

    public static String getUserMessage(Context context, AylaError error, int messageUnknownErrorResId) {
        return getUserMessage(error, context.getResources().getString(messageUnknownErrorResId));
    }

    public static String getUserMessage(AylaError error, String messageUnknownError) {
        Log.e("AMAP5-ERR", "Unhandled AylaError: ", error);

        if (error instanceof ServerError) {
            ServerError serverError = ((ServerError) error);
            int code = serverError.getServerResponseCode();

            // TODO: Localize these by putting in R.string resource
            switch (code) {
                case SERVER_ERROR_NOT_FOUND:
                    return "The requested resource was not found. " + messageUnknownError;

                case SERVER_ERROR_TIMEOUT:
                    return "Server operation is taking too long. " + messageUnknownError;

                case SERVER_ERROR_TOKEN_EXPIRE:
                    return "The token has expired. " + messageUnknownError;

                default:
                    if (serverError.getServerResponseData() != null && serverError.getServerResponseData().length > 0) {
                        return "Response Code: " + code + "\n" + new String(serverError.getServerResponseData()) + ";\n" + messageUnknownError;
                    }

                    return "Unknown server or network error. " + messageUnknownError;
            }
        }

        if (error.getLocalizedMessage() != null && !error.getLocalizedMessage().equals("")) {
            return error.getLocalizedMessage() + ";\n" + messageUnknownError;
        }

        return messageUnknownError;
    }

}
