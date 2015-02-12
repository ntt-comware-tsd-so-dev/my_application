package com.aylanetworks.agilelink.fragments;

import android.app.ActionBar;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.aaml.AylaUser;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.SessionManager;

import java.lang.ref.WeakReference;

/**
 * Created by Brian King on 1/20/15.
 */
public class SignUpDialog extends Dialog implements View.OnClickListener {

    public interface SignUpListener {
        void signUpSucceeded(AylaUser newUser);
    }

    private SignUpListener _signUpListener;

    public SignUpDialog(final Context context, SignUpListener listener) {
        super(context, R.style.FullHeightDialog);
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
        Button b = (Button)findViewById(R.id.btnSignUp);
        b.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
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
        user.email = email.getText().toString();
        user.password = password.getText().toString();
        user.firstname = firstName.getText().toString();
        user.lastname = lastName.getText().toString();
        user.country = country.getText().toString();
        user.zip = zip.getText().toString();
        user.phone = phone.getText().toString();
        user.aylaDevKitNum = evb.getText().toString();

        progress.setVisibility(View.VISIBLE);

        SessionManager.registerNewUser(user, new RegisterNewUserHandler(this));
    }

    static class RegisterNewUserHandler extends Handler {
        private WeakReference<SignUpDialog> _signUpDialog;
        public RegisterNewUserHandler(SignUpDialog signUpDialog) {
            _signUpDialog = new WeakReference<SignUpDialog>(signUpDialog);
        }

        @Override
        public void handleMessage(Message msg) {
            // Get our views
            EditText password = (EditText)_signUpDialog.get().findViewById(R.id.etPassword);
            EditText confirm = (EditText)_signUpDialog.get().findViewById(R.id.etConfirmPassword);
            EditText email = (EditText)_signUpDialog.get().findViewById(R.id.etEmail);
            EditText firstName = (EditText)_signUpDialog.get().findViewById(R.id.etFirstName);
            EditText lastName = (EditText)_signUpDialog.get().findViewById(R.id.etLastName);
            EditText country = (EditText)_signUpDialog.get().findViewById(R.id.etCountry);
            EditText zip = (EditText)_signUpDialog.get().findViewById(R.id.etZipCode);
            EditText phone = (EditText)_signUpDialog.get().findViewById(R.id.etPhoneNumber);
            EditText evb = (EditText)_signUpDialog.get().findViewById(R.id.etEvbNumber);
            ProgressBar progress = (ProgressBar)_signUpDialog.get().findViewById(R.id.pbProgressBar);

            progress.setVisibility(View.GONE);
            if ( msg.what == AylaNetworks.AML_ERROR_OK ) {
                AylaUser newUser = AylaSystemUtils.gson.fromJson((String)msg.obj, AylaUser.class);
                if ( _signUpDialog.get()._signUpListener != null ) {
                    _signUpDialog.get()._signUpListener.signUpSucceeded(newUser);
                    _signUpDialog.get().dismiss();
                }
            } else {
                if ( msg.arg1 == AylaNetworks.AML_USER_INVALID_PARAMETERS ) {
                    // Put a message together with more details
                    AylaUser aylaUser = AylaSystemUtils.gson.fromJson((String)msg.obj, AylaUser.class);
                    String errMsg = null;

                    if (aylaUser.email != null) {
                        email.requestFocus();
                        errMsg = email.getHint() + " " + aylaUser.email; // contains error message
                    } else
                    if (aylaUser.password != null) {
                        password.requestFocus();
                        errMsg = password.getHint() + " " + aylaUser.password;
                    } else
                    if (aylaUser.firstname != null) {
                        firstName.requestFocus();
                        errMsg = firstName.getHint() + " " + aylaUser.firstname; // contains error message
                    } else
                    if (aylaUser.lastname != null) {
                        lastName.requestFocus();
                        errMsg = lastName.getHint() + " " + aylaUser.lastname; // contains error message
                    } else
                    if (aylaUser.country != null) {
                        country.requestFocus();
                        errMsg = country.getHint() + " " + aylaUser.country; // contains error message
                    }

                    if (errMsg != null) {
                        Toast.makeText(_signUpDialog.get().getContext(), errMsg, Toast.LENGTH_LONG).show();
                    }
                } else {
                    String errMsg = _signUpDialog.get().getContext().getResources().getString(R.string.sign_up_error);
                    errMsg = errMsg + ":\n" + msg.obj;
                    Toast.makeText(_signUpDialog.get().getContext(), errMsg, Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}
