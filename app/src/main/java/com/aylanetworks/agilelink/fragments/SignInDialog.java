package com.aylanetworks.agilelink.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
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
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaReachability;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.aaml.AylaUser;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.SessionManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

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
    private TextView _resendEmailTextView;
    private TextView _forgotPasswordTextView;
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
        _resendEmailTextView = (TextView)view.findViewById(R.id.resendConfirmationTextView);
        _forgotPasswordTextView = (TextView)view.findViewById(R.id.forgot_password);
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
        _resendEmailTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onResendEmail();
            }
        });
        _forgotPasswordTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onForgotPassword();
            }
        });

        Bundle args = getArguments();
        String username = args.getString(ARG_USERNAME);
        String password = args.getString(ARG_PASSWORD);
        _username.setText(username);
        _password.setText(password);

        return view;
    }

    public void setUsername(String username) {
        _username.setText(username);
        _password.setText("");
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

    private void onResendEmail(){
        final EditText emailEditText = new EditText(getActivity());
        Dialog dlg = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.resend_confirmation)
                .setMessage(R.string.resend_confirmation_message)
                .setView(emailEditText)
                .setPositiveButton(R.string.send, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SessionManager.SessionParameters sessionParams = SessionManager.sessionParameters();
                        Map<String, String> params = new HashMap<>();
                        if (sessionParams.registrationEmailTemplateId == null) {
                            params.put(AylaNetworks.AML_EMAIL_BODY_HTML, sessionParams.registrationEmailBodyHTML);
                        } else {
                            params.put(AylaNetworks.AML_EMAIL_TEMPLATE_ID, sessionParams.registrationEmailTemplateId);
                        }
                        params.put(AylaNetworks.AML_EMAIL_SUBJECT, sessionParams.registrationEmailSubject);
                        AylaUser.resendConfirmation(new Handler() {
                            @Override
                            public void handleMessage(Message msg) {
                                Log.d(LOG_TAG, "Resend email result: " + msg);

                                if (msg.what == AylaNetworks.AML_ERROR_OK) {
                                    Toast.makeText(getActivity(), R.string.email_confirmation_sent, Toast.LENGTH_LONG).show();
                                } else {
                                    // Get the error out of the message if we can
                                    String json = (String) msg.obj;
                                    String errorMessage = null;
                                    try {
                                        JSONObject result = new JSONObject(json);
                                        String errorJSON = result.getString("errors");
                                        JSONObject errors = new JSONObject(errorJSON);
                                        errorMessage = errors.getString("email");
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }

                                    if (errorMessage == null) {
                                        errorMessage = getResources().getString(R.string.error_account_confirm_failed);
                                    }

                                    Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_LONG).show();
                                }
                            }
                        }, emailEditText.getText().toString(), sessionParams.appId, sessionParams.appSecret, params);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dlg.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dlg.show();
    }

    private void onForgotPassword() {
        final EditText emailEditText = new EditText(getActivity());
        Dialog dlg = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.forgot_password_title)
                .setMessage(R.string.forgot_password_message)
                .setView(emailEditText)
                .setPositiveButton(R.string.send, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SessionManager.SessionParameters sessionParams = SessionManager.sessionParameters();
                        Map<String, String> params = new HashMap<>();
                        if (sessionParams.registrationEmailTemplateId == null) {
                            params.put(AylaNetworks.AML_EMAIL_BODY_HTML, sessionParams.resetPasswordEmailBodyHTML);
                        } else {
                            params.put(AylaNetworks.AML_EMAIL_TEMPLATE_ID, sessionParams.resetPasswordEmailTemplateId);
                        }
                        params.put(AylaNetworks.AML_EMAIL_SUBJECT, sessionParams.resetPasswordEmailSubject);
                        AylaUser.resetPassword(new Handler() {
                            @Override
                            public void handleMessage(Message msg) {
                                Log.d(LOG_TAG, "Reset password result: " + msg);

                                if (msg.what == AylaNetworks.AML_ERROR_OK) {
                                    Toast.makeText(getActivity(), R.string.password_reset_sent, Toast.LENGTH_LONG).show();
                                } else {
                                    // Get the error out of the message if we can
                                    String json = (String) msg.obj;
                                    String errorMessage = null;
                                    try {
                                        JSONObject result = new JSONObject(json);
                                        String errorJSON = result.getString("errors");
                                        JSONObject errors = new JSONObject(errorJSON);
                                        errorMessage = errors.getString("email");
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }

                                    if (errorMessage == null) {
                                        errorMessage = getResources().getString(R.string.error_password_reset_failed);
                                    }

                                    Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_LONG).show();
                                }
                            }
                        }, emailEditText.getText().toString(), sessionParams.appId, sessionParams.appSecret, params);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dlg.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dlg.show();
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

    static class OauthHandler extends Handler {
        private WeakReference<SignInDialog> _signInDialog;
        public OauthHandler(SignInDialog signInDialog) {
            _signInDialog = new WeakReference<SignInDialog>(signInDialog);
        }
        @Override
        public void handleMessage(Message msg) {
            Log.d(LOG_TAG, "OAUTH response: " + msg);
            _signInDialog.get()._webView.setVisibility(View.GONE);
            _signInDialog.get()._loginButton.setVisibility(View.VISIBLE);

            if ( msg.what == AylaNetworks.AML_ERROR_OK ) {
                _signInDialog.get()._listener.signInOAuth(msg);
            }
        }
    }

    private OauthHandler _oauthHandler = new OauthHandler(this);
}
