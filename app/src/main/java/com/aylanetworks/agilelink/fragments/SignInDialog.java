package com.aylanetworks.agilelink.fragments;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.aylanetworks.agilelink.R;

/**
 * Created by Brian King on 1/30/15.
 */
public class SignInDialog extends DialogFragment {
    public static final String ARG_USERNAME = "username";
    public static final String ARG_PASSWORD = "password";
    public static final String OAUTH_GOOGLE = "google_provider";
    public static final String OAUTH_FACEBOOK = "facebook_provider";

    public interface SignInDialogListener {
        void signIn(String username, String password);
        void signInOAuth(String type);
        void signUp();
    }

    private EditText _username;
    private EditText _password;
    private Button _loginButton;
    private ImageButton _googleLoginButton;
    private ImageButton _facebookLoginButton;
    private TextView _signUpTextView;
    private SignInDialogListener _listener;

    public SignInDialog() {
        // Empty constructor required
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.login, container);
        _username = (EditText)view.findViewById(R.id.userNameEditText);
        _password = (EditText)view.findViewById(R.id.passwordEditText);
        _loginButton = (Button)view.findViewById(R.id.buttonSignIn);
        _facebookLoginButton = (ImageButton)view.findViewById(R.id.facebook_login);
        _googleLoginButton = (ImageButton)view.findViewById(R.id.google_login);
        _signUpTextView = (TextView)view.findViewById(R.id.signUpTextView);

        _loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _listener.signIn(_username.getText().toString(), _password.getText().toString());
            }
        });

        _facebookLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _listener.signInOAuth(OAUTH_FACEBOOK);
            }
        });

        _googleLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _listener.signInOAuth(OAUTH_GOOGLE);
            }
        });

        _signUpTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _listener.signUp();
            }
        });

        Bundle args = getArguments();
        String username = args.getString(ARG_USERNAME);
        String password = args.getString(ARG_PASSWORD);
        _username.setText(username);
        _password.setText(password);

        return view;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        getActivity().finish();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        _listener = (SignInDialogListener)activity;
    }
}
