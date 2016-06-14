package com.aylanetworks.agilelink;

import android.content.Context;
import android.content.res.Resources;

import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ServerError;

/**
 * Created by KLiu on 6/14/16.
 */
public class ErrorUtils {

    private final static int SERVER_ERROR_TOKEN_EXPIRE = 403;
    private final static int SERVER_ERROR_NOT_FOUND = 404; // No results were found
    private final static int SERVER_ERROR_TIMEOUT = 408; // Request timeout

    public static String getUserMessage(Context context, AylaError error, int messageUnknownErrorResId) {
        return getUserMessage(context, error, context.getResources().getString(messageUnknownErrorResId));
    }

    public static String getUserMessage(Context context, AylaError error, String messageUnknownError) {
        if (error instanceof ServerError) {
            int code = ((ServerError) error).getServerResponseCode();
            Resources res = context.getResources();

            // TODO: Localize these by putting in R.string resource
            switch (code) {
                case SERVER_ERROR_NOT_FOUND:
                    return "The requested resource was not found.";

                case SERVER_ERROR_TIMEOUT:
                    return "Server operation is taking too long.";

                case SERVER_ERROR_TOKEN_EXPIRE:
                    return "The token has expired.";

                default:
                    return messageUnknownError;
            }
        }

        return messageUnknownError;
    }

}
