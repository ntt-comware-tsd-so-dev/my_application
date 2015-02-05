package com.aylanetworks.agilelink.fragments;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaReachability;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.aaml.AylaUser;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.SessionManager;

/**
 * Created by Brian King on 1/30/15.
 */
public class SignInDialog extends DialogFragment {
    public static final String ARG_USERNAME = "username";
    public static final String ARG_PASSWORD = "password";
    public static final String OAUTH_GOOGLE = "google_provider";
    public static final String OAUTH_FACEBOOK = "facebook_provider";

    private static final String LOG_TAG="SignInDialog";

    public interface SignInDialogListener {
        void signIn(String username, String password);
        void signInOAuth(Message msg);
        void signUp();
    }

    private EditText _username;
    private EditText _password;
    private Button _loginButton;
    private ImageButton _googleLoginButton;
    private ImageButton _facebookLoginButton;
    private TextView _signUpTextView;
    private WebView _webView;
    private SignInDialogListener _listener;

    public SignInDialog() {
        AylaReachability.determineReachability(true);
        AylaUser aylaUser = new AylaUser();
        AylaUser.setCurrent(aylaUser);

        aylaUser.setauthHeaderValue("none");
        aylaUser.setExpiresIn(0);
        aylaUser.setRefreshToken("");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
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
        _webView = (WebView)view.findViewById(R.id.webview);

        _loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _listener.signIn(_username.getText().toString(), _password.getText().toString());
            }
        });

        _facebookLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                oAuthSignIn(OAUTH_FACEBOOK);
            }
        });

        _googleLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                oAuthSignIn(OAUTH_GOOGLE);
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

    private void oAuthSignIn(String service) {
        _webView.setVisibility(View.VISIBLE);

        String serviceName = (service.equals(OAUTH_FACEBOOK)) ? getString(R.string.facebook) :
                getString(R.string.google);

        // Clear out any previous contents of the webview
        String webViewEmptyHTML = getActivity().getResources().getString(R.string.oauth_empty_html);
        webViewEmptyHTML = webViewEmptyHTML.replace("[[PROVIDER]]", serviceName);
        _webView.loadDataWithBaseURL("", webViewEmptyHTML, "text/html", "UTF-8", "");
        _webView.bringToFront();

        _loginButton.setVisibility(View.INVISIBLE);
        AylaSystemUtils.serviceReachableTimeout = AylaNetworks.AML_SERVICE_REACHABLE_TIMEOUT;
        SessionManager.SessionParameters params = SessionManager.sessionParameters();
        AylaUser.loginThroughOAuth(_oauthHandler, service, _webView, params.appId, params.appSecret);
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

    private Handler _oauthHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.d(LOG_TAG, "OAUTH response: " + msg);
            _webView.setVisibility(View.GONE);
            _loginButton.setVisibility(View.VISIBLE);

            if ( msg.what == AylaNetworks.AML_ERROR_OK ) {
                _listener.signInOAuth(msg);
            }
        }
    };
}
