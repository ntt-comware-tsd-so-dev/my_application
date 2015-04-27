package com.aylanetworks.agilelink;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

/*
 * AccountConfirmActivity.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 2/9/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */
public class AccountConfirmActivity extends Activity {
    private final static String LOG_TAG = "AccountConfirmActivity";

    public final static String URL_SCHEME = "aylacontrol";
    public final static String ARG_ACCOUNT_CONFIRM = "account_confirm";
    public final static String ARG_ACCOUNT_CONFIRM_INTENT = "account_confirm_intent";

    static Uri uri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //capture intent from link in confirmation email and send it to the login activity

        Intent intent = getIntent();

        if (intent.hasCategory(Intent.CATEGORY_BROWSABLE) && intent.getScheme().equals(URL_SCHEME)){
            Intent startIntent = new Intent(AccountConfirmActivity.this, SignInActivity.class);
            startIntent.putExtra(ARG_ACCOUNT_CONFIRM, true);
            startIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startIntent.putExtra(ARG_ACCOUNT_CONFIRM_INTENT, intent);

            uri = getIntent().getData();	// save tokens, if any
            Log.d(LOG_TAG, "URI: " + uri);
            startActivity(startIntent);
            AccountConfirmActivity.this.finish();
        }
    }
}
