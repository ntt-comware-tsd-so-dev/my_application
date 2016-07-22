package com.aylanetworks.agilelink.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.Response;
import com.aylanetworks.agilelink.framework.AMAPCore;
import com.aylanetworks.aylasdk.AylaEmailTemplate;
import com.aylanetworks.aylasdk.AylaNetworks;
import com.aylanetworks.aylasdk.AylaUser;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;

import java.lang.ref.WeakReference;

/*
 * SignUpDialog.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 1/20/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class SignUpDialog extends Dialog implements View.OnClickListener {

    public interface SignUpListener {
        void signUpSucceeded(AylaUser newUser);
    }

    private SignUpListener _signUpListener;

    public SignUpDialog(final Context context, SignUpListener listener) {
        super(context, R.style.AppTheme);
        _signUpListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.sign_up);

        // Make the dialog full-screen
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.MATCH_PARENT;
        getWindow().setAttributes(params);

        // Set up a handler for our button
        findViewById(R.id.btnSignUp).setOnClickListener(this);
        findViewById(R.id.imageViewConnected).setOnClickListener(this);
    }

    void onButtonSignUpClicked() {
        // Get our views
        EditText password = (EditText)findViewById(R.id.etPassword);
        EditText confirm = (EditText)findViewById(R.id.etConfirmPassword);
        EditText email = (EditText)findViewById(R.id.etEmail);
        EditText firstName = (EditText)findViewById(R.id.etFirstName);
        EditText lastName = (EditText)findViewById(R.id.etLastName);
        EditText country = (EditText)findViewById(R.id.etCountry);
        EditText zip = (EditText)findViewById(R.id.etZipCode);
        EditText phone = (EditText)findViewById(R.id.etPhoneNumber);
        EditText evb = (EditText)findViewById(R.id.etEvbNumber);

        ProgressBar progress = (ProgressBar)findViewById(R.id.pbProgressBar);

        // Validate
        Resources res = getContext().getResources();

        // Password: Exists and matches
        if ( password.toString().length() == 0 ) {
            Toast.makeText(getContext(), R.string.password_required, Toast.LENGTH_SHORT).show();
            return;
        }

        if ( !password.getText().toString().equals(confirm.getText().toString()) ) {
            Toast.makeText(getContext(), R.string.password_no_match, Toast.LENGTH_SHORT).show();
            return;
        }

        AylaUser user = new AylaUser();
        user.setEmail(email.getText().toString());
        user.setPassword(password.getText().toString());
        user.setFirstname(firstName.getText().toString());
        user.setLastname(lastName.getText().toString());
        user.setCountry(country.getText().toString());
        user.setZip(zip.getText().toString());
        user.setPhone(phone.getText().toString());
        user.setAylaDevKitNum(evb.getText().toString());

        progress.setVisibility(View.VISIBLE);

        AMAPCore.SessionParameters sessionParams = AMAPCore.sharedInstance().getSessionParameters();
        AylaEmailTemplate template = new AylaEmailTemplate();
        template.setEmailSubject(sessionParams.registrationEmailSubject);
        template.setEmailTemplateId(sessionParams.registrationEmailTemplateId);
        template.setEmailBodyHtml(sessionParams.registrationEmailBodyHTML);

        AylaNetworks.sharedInstance().getLoginManager().signUp(user, template,
                new Response.Listener<AylaUser>() {
                    @Override
                    public void onResponse(AylaUser newUser) {
                        if ( _signUpListener != null ) {
                            _signUpListener.signUpSucceeded(newUser);
                            dismiss();
                        }
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        String errMsg = getContext().getResources().getString(R.string.sign_up_error);
                        Toast.makeText(getContext(), errMsg, Toast.LENGTH_LONG).show();
                    }
                });

    }

    // If you are testing add & delete account, then put your info here
    void onImageViewConnectedClicked() {
        /*
        ((EditText)findViewById(R.id.etEmail)).setText("");
        ((EditText)findViewById(R.id.etPassword)).setText("");
        ((EditText)findViewById(R.id.etConfirmPassword)).setText("");
        ((EditText)findViewById(R.id.etFirstName)).setText("");
        ((EditText)findViewById(R.id.etLastName)).setText("");
        ((EditText)findViewById(R.id.etCountry)).setText("USA");
        ((EditText)findViewById(R.id.etZipCode)).setText("94089");
        ((EditText)findViewById(R.id.etPhoneNumber)).setText("6505551212");
        */
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnSignUp:
                onButtonSignUpClicked();
                break;
            case R.id.imageViewConnected:
                onImageViewConnectedClicked();
                break;
        }
    }
}
