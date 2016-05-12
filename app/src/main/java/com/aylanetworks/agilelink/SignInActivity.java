package com.aylanetworks.agilelink;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.aylanetworks.aylasdk.AylaNetworks;
import com.aylanetworks.aylasdk.AylaSystemUtils;
import com.aylanetworks.aylasdk.AylaUser;
import com.aylanetworks.agilelink.fragments.ResetPasswordDialog;
import com.aylanetworks.agilelink.fragments.SignUpDialog;
import com.aylanetworks.agilelink.framework.SessionManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/*
 * SignInActivity.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 1/30/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class SignInActivity extends FragmentActivity implements SignUpDialog.SignUpListener, SessionManager.SessionListener {
    public static final String ARG_LOGIN_TYPE = "loginType";
    public static final String ARG_USERNAME = "username";
    public static final String ARG_PASSWORD = "password";
    public static final String ARG_OAUTH_MESSAGE = "oauth_msg";
    public static final String ARG_MESSAGE_OBJECT = "msg_obj";

    public static final int LOGIN_TYPE_PASSWORD = 1;
    public static final int LOGIN_TYPE_OAUTH = 2;
    public static final int LOGIN_TYPE_SIGN_UP = 3;
    public static final int LOGIN_TYPE_FORGOT_PASSWORD = 4;
    public static final int LOGIN_TYPE_RESEND_CONFIRMATION = 5;

    public static final String OAUTH_GOOGLE = "google_provider";
    public static final String OAUTH_FACEBOOK = "facebook_provider";

    private static final String SIGNUP_TOKEN = "user_sign_up_token";
    private static final String RESET_PASSWORD_TOKEN = "user_reset_password_token";

    private static final String LOG_TAG = "SignInDialog";

    private EditText _username;
    private EditText _password;
    private Button _loginButton;
    private ImageButton _googleLoginButton;
    private ImageButton _facebookLoginButton;
    private TextView _signUpTextView;
    private TextView _resendEmailTextView;
    private TextView _forgotPasswordTextView;
    private WebView _webView;
    private Spinner _serviceTypeSpinner;

    private final static String _serviceTypes[] = {"Device", "Field", "Production", "Staging", "Demo"};

    public SignInActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, "nod: onCreate");

        // Phones are portrait-only. Tablets support orientation changes.
        if(getResources().getBoolean(R.bool.portrait_only)){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        setContentView(R.layout.login);

        _username = (EditText) findViewById(R.id.userNameEditText);
        _password = (EditText) findViewById(R.id.passwordEditText);
        _loginButton = (Button) findViewById(R.id.buttonSignIn);
        _facebookLoginButton = (ImageButton) findViewById(R.id.facebook_login);
        _googleLoginButton = (ImageButton) findViewById(R.id.google_login);
        _signUpTextView = (TextView) findViewById(R.id.signUpTextView);
        _resendEmailTextView = (TextView) findViewById(R.id.resendConfirmationTextView);
        _forgotPasswordTextView = (TextView) findViewById(R.id.forgot_password);
        _webView = (WebView) findViewById(R.id.webview);
        _serviceTypeSpinner = (Spinner) findViewById(R.id.service_type_spinner);

        // The serviceTypeSpinner is only shown if the user taps "Forgot Password" and enters
        // "aylarocks" for the email address. This is a developer-only spinner.
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.spinner_large_text, _serviceTypes);
        _serviceTypeSpinner.setAdapter(adapter);
        _serviceTypeSpinner.setSelection(AylaSystemUtils.serviceType);

        // We need to do this in a runnable so we don't get the first onItemSelected call from
        // the above call to setSelection.
        _serviceTypeSpinner.postDelayed(new Runnable() {
            @Override
            public void run() {
                _serviceTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        switch (position) {
                            case AylaNetworks.AML_PRODUCTION_SERVICE:
                                SessionManager.getInstance().setServiceType(AylaNetworks.AML_PRODUCTION_SERVICE);
                                Toast.makeText(MainActivity.getInstance(), "Production Service", Toast.LENGTH_LONG).show();
                                break;

                            case AylaNetworks.AML_STAGING_SERVICE:
                                SessionManager.getInstance().setServiceType(AylaNetworks.AML_STAGING_SERVICE);
                                Toast.makeText(MainActivity.getInstance(), "Staging Service", Toast.LENGTH_LONG).show();
                                break;

                            default:
                                String message = "No app ID for " + _serviceTypes[position] +
                                        " service, but I'll set the type anyway. You probably can't log in.";
                                Log.e(LOG_TAG, message);
                                Toast.makeText(MainActivity.getInstance(), message, Toast.LENGTH_SHORT).show();

                                // The positions in our array happen to coincide with the service types they represent.
                                SessionManager.getInstance().setServiceType(position);
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });
            }
        }, 500);

        _loginButton.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                                imm.hideSoftInputFromWindow(_loginButton.getWindowToken(), 0);
                                                showSigningInDialog();
                                                SessionManager.startSession(_username.getText().toString(), _password.getText().toString());
                                            }
                                        }
        );

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
                                                       Log.i(LOG_TAG, "Sign up");
                                                       SignUpDialog d = new SignUpDialog(SignInActivity.this, SignInActivity.this);
                                                       d.show();
                                                   }});

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

        Intent intent = getIntent();
        //dumpIntent("nod: ", intent);
        Bundle args = intent.getExtras();
        if (args != null) {
            String username = args.getString(ARG_USERNAME);
            String password = args.getString(ARG_PASSWORD);
            _username.setText(username);
            _password.setText(password);
            _password.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        // Enter key pressed
                        _loginButton.performClick();
                        return true;
                    }
                    return false;
                }
            });
        }

        SessionManager.addSessionListener(this);
    }

    @Override
    protected void onDestroy() {
        Log.d(LOG_TAG, "nod: onDestroy");
        if ( _progressDialog != null ) {
            _progressDialog.dismiss();
        }
        SessionManager.removeSessionListener(this);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "nod: onResume");

        mContext = this;

        Uri uri = AccountConfirmActivity.uri;
        if (uri != null) {
            Log.i(LOG_TAG, "nod: onResume URI is " + uri);
            handleOpenURI(uri);
            // Clear out the URI
            AccountConfirmActivity.uri = null;
        }
    }

    private ProgressDialog _progressDialog;
    void showSigningInDialog() {
        if ( _progressDialog != null ) {
            _progressDialog.dismiss();
        }

        String title = getResources().getString(R.string.signIn);
        String message = getResources().getString(R.string.signingIn);
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setTitle(title);
        dialog.setIcon(R.drawable.ic_launcher);
        dialog.setMessage(message);
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        dialog.setOnCancelListener(null);
        dialog.show();
        _progressDialog = dialog;
    }

    @Override
    public void onBackPressed() {
        if ( getSupportFragmentManager().getBackStackEntryCount() == 0 ) {
            Intent intent = new Intent();
            setResult(RESULT_FIRST_USER, intent);
            finish();
        } else {
            super.onBackPressed();
        }
    }



    private void oAuthSignIn(String service) {
        _webView.setVisibility(View.VISIBLE);
        CookieManager.getInstance().removeAllCookie();

        String serviceName = (service.equals(OAUTH_FACEBOOK)) ? getString(R.string.facebook) :
                getString(R.string.google);

        // Clear out any previous contents of the webview
        String webViewEmptyHTML = this.getResources().getString(R.string.oauth_empty_html);
        webViewEmptyHTML = webViewEmptyHTML.replace("[[PROVIDER]]", serviceName);
        _webView.loadDataWithBaseURL("", webViewEmptyHTML, "text/html", "UTF-8", "");
        _webView.bringToFront();

        _loginButton.setVisibility(View.INVISIBLE);
        AylaSystemUtils.serviceReachableTimeout = AylaNetworks.AML_SERVICE_REACHABLE_TIMEOUT;
        SessionManager.SessionParameters params = SessionManager.sessionParameters();
        AylaUser.loginThroughOAuth(_oauthHandler, service, _webView, params.appId, params.appSecret);
    }

    private void onResendEmail() {
        final EditText emailEditText = new EditText(this);
        emailEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailEditText.setText(_username.getText());
        Dialog dlg = new AlertDialog.Builder(this)
                .setIcon(R.drawable.ic_launcher)
                .setTitle(R.string.resend_confirmation)
                .setMessage(R.string.resend_confirmation_message)
                .setView(emailEditText)
                .setPositiveButton(R.string.send, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(emailEditText.getWindowToken(), 0);

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

                                if (AylaNetworks.succeeded(msg)) {
                                    Toast.makeText(SignInActivity.this, R.string.email_confirmation_sent, Toast.LENGTH_LONG).show();
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

                                    Toast.makeText(SignInActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                                }
                            }
                        }, emailEditText.getText().toString(), sessionParams.appId, sessionParams.appSecret, params);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(emailEditText.getWindowToken(), 0);
                    }
                })
                .create();
        dlg.show();

        InputMethodManager imm = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        emailEditText.requestFocus();
    }

    private void onForgotPassword() {
        final EditText emailEditText = new EditText(this);
        emailEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailEditText.setText(_username.getText());
        final Dialog dlg = new AlertDialog.Builder(this)
                .setIcon(R.drawable.ic_launcher)
                .setTitle(R.string.forgot_password_title)
                .setMessage(R.string.forgot_password_message)
                .setView(emailEditText)
                .setPositiveButton(R.string.send, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Developer UI Enable check
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(emailEditText.getWindowToken(), 0);

                        String email = emailEditText.getText().toString();
                        if ("aylarocks".compareToIgnoreCase(email) == 0) {
                            // Enable the dev UI
                           /* _serviceTypeSpinner.setVisibility(View.VISIBLE);
                            Toast.makeText(MainActivity.getInstance(), "Developer mode enabled", Toast.LENGTH_LONG).show();
                            return;*/

                           if(BuildConfig.DEBUG){
                               Intent intent = new Intent(SignInActivity.this, DeveloperOptions.class);
                               startActivity(intent);
                               return;
                           }
                        }

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

                                if (AylaNetworks.succeeded(msg)) {
                                    Toast.makeText(SignInActivity.this, R.string.password_reset_sent, Toast.LENGTH_LONG).show();
                                    _username.setText(emailEditText.getText().toString());
                                } else {
                                    // Get the error out of the message if we can
                                    String json = (String) msg.obj;
                                    String errorMessage = null;
                                    try {
                                        if (json != null) {
                                            JSONObject result = new JSONObject(json);
                                            String errorJSON = result.getString("errors");
                                            JSONObject errors = new JSONObject(errorJSON);
                                            errorMessage = errors.getString("email");
                                        }

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }

                                    if (errorMessage == null) {
                                        errorMessage = getResources().getString(R.string.error_password_reset_failed);
                                    }

                                    Toast.makeText(SignInActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                                }
                            }
                        }, emailEditText.getText().toString(), sessionParams.appId, sessionParams.appSecret, params);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(emailEditText.getWindowToken(), 0);
                    }
                })
                .create();
        dlg.show();

        InputMethodManager imm = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        emailEditText.requestFocus();
    }

    @Override
    public void signUpSucceeded(AylaUser newUser) {
        Toast.makeText(this, R.string.sign_up_success, Toast.LENGTH_LONG).show();
        _username.setText(newUser.email);
        _password.setText(newUser.password);
    }

    private void handleOpenURI(Uri uri) {
        // sign-up confirmation:
        // aylacontrol://user_sign_up_token?token=pdsWFmcU

        // Reset password confirmation:
        // aylacontrol://user_reset_password_token?token=3DrjCTqs

        String path = uri.getLastPathSegment();
        if (path == null) {
            // Some URIs are formatted without a path after the host. Just use the hostname in
            // this case.
            path = uri.getHost();
        }
        String query = uri.getEncodedQuery();
        String parts[] = null;
        if (query != null) {
            parts = query.split("=");
        }

        if (path.equals(SIGNUP_TOKEN)) {
            if (parts == null || parts.length != 2 || !parts[0].equals("token")) {
                // Unknown query string
                Log.e(LOG_TAG, "nod: the URL couldn't be opened [" + uri + "]");
                Toast.makeText(this, R.string.error_open_uri, Toast.LENGTH_SHORT).show();
            } else {
                Log.d(LOG_TAG, "nod: handleUserSignupToken [" + parts[1] + "]");
                handleUserSignupToken(parts[1]);
            }
        } else if (path.equals(RESET_PASSWORD_TOKEN)) {
            if (parts == null || parts.length != 2 || !parts[0].equals("token")) {
                // Unknown query string
                Log.e(LOG_TAG, "nod: the URL couldn't be opened [" + uri + "]");
                Toast.makeText(this, R.string.error_open_uri, Toast.LENGTH_SHORT).show();
            } else {
                handleUserResetPasswordToken(parts[1]);
            }
        } else {
            Log.e(LOG_TAG, "nod: Unknown URI: " + uri);
        }
    }

    SignUpConfirmationHandler _signUpConfirmationHandler;

    private void handleUserSignupToken(String token) {
        Log.d(LOG_TAG, "nod: handleUserSignupToken: " + token);

        // authenticate the token
        Map<String, String> callParams = new HashMap<String, String>();
        callParams.put("confirmation_token", token); // required
        _signUpConfirmationHandler = new SignUpConfirmationHandler(this);
        AylaUser.signUpConfirmation(_signUpConfirmationHandler, callParams);
    }

    private void handleUserResetPasswordToken(String token) {
        Log.i(LOG_TAG, "nod: handleUserResetPasswordToken: " + token);
        ResetPasswordDialog d = new ResetPasswordDialog();
        d.setToken(token);
        d.show(getSupportFragmentManager(), "reset_password");
    }

    @Override
    public void loginStateChanged(boolean loggedIn, AylaUser aylaUser) {
        Log.d(LOG_TAG, "nod: Login state changed. Logged in: " + loggedIn);
        if ( _progressDialog != null ) {
            _progressDialog.dismiss();
        }
        if (loggedIn) {
            finish();
        }
    }

    @Override
    public void reachabilityChanged(int reachabilityState) { }

    @Override
    public void lanModeChanged(boolean lanModeEnabled) { }

    void confirmationComplete(Message msg) {
        String jsonResults = (String) msg.obj;
        if (AylaNetworks.succeeded(msg)) {
            // save auth info of current user
            AylaUser aylaUser = AylaSystemUtils.gson.fromJson(jsonResults, AylaUser.class);
            AylaSystemUtils.saveSetting(SessionManager.AYLA_SETTING_CURRENT_USER, jsonResults);
            String toastMessage = getString(R.string.welcome_new_account);
            Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
            _username.setText(aylaUser.email);
            _password.setText(aylaUser.password);
            Log.d(LOG_TAG, "nod: SignUpConfirmationHandler set user & password");
        } else {
            AylaSystemUtils.saveToLog("%s, %s, %s:%s, %s", "E", "amca.signin", "userSignUpConfirmation", "Failed", "userSignUpConfirmation_handler");
            int resID;
            if (msg.arg1 == 422) {
                resID = R.string.error_invalid_token; // Invalid token
            } else {
                resID = R.string.error_account_confirm_failed; // Unknown error occurred
            }
            AlertDialog.Builder ad = new AlertDialog.Builder(this);
            ad.setIcon(R.drawable.ic_launcher);
            ad.setTitle(R.string.error_sign_up_title);
            ad.setMessage(resID);
            ad.setPositiveButton(android.R.string.ok, null);
            ad.show();
        }
    }

    static class SignUpConfirmationHandler extends Handler {
        private WeakReference<SignInActivity> _activity;
        public SignUpConfirmationHandler(SignInActivity activity) {
            _activity = new WeakReference<SignInActivity>(activity);
        }

        public void handleMessage(final Message msg) {
            // clear sign-up token
            Log.d(LOG_TAG, "nod: SignUpConfirmationHandler [" + msg + "]");
            _activity.get().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    _activity.get().confirmationComplete(msg);
                }
            });
        }
    }

    static class OauthHandler extends Handler {
        private WeakReference<SignInActivity> _signInActivity;

        public OauthHandler(SignInActivity signInActivity) {
            _signInActivity = new WeakReference<SignInActivity>(signInActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(LOG_TAG, "OAUTH response: " + msg);
            _signInActivity.get()._webView.setVisibility(View.GONE);
            _signInActivity.get()._loginButton.setVisibility(View.VISIBLE);
            if (AylaNetworks.succeeded(msg)) {
                SessionManager.startOAuthSession(msg);
            } else {
                String errInfo = null;
                try {
                    JSONObject json = new JSONObject((String)msg.obj);
                    String error = json.getString("error");
                    if (!TextUtils.isEmpty(error)) {
                        errInfo = error;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                if (TextUtils.isEmpty(errInfo)) {
                    errInfo = getContext().getString(R.string.login_failed);
                }

                Toast.makeText(
                        SignInActivity.getContext()
                        , SignInActivity.getLocalizedString(errInfo)
                        , Toast.LENGTH_LONG)
                        .show();
            }
        }
    }

    private OauthHandler _oauthHandler = new OauthHandler(this);

    private static Context mContext;
    static Context getContext() {
        return mContext;
    }

    /**
     * Match a String and return a resource ID, which reference a localized string resource.
     *
     * @param str the string to be matched
     * @return the string resId, default to R.string.unauthorized as it serves login originally.
     * */ //TODO: This is originally for login only, now it covers more than that. Find a proper
    // place for it.
    public static int getLocalizedString(final String str) {
        if (TextUtils.isEmpty(str)) {
            return R.string.unauthorized;
        }
        if ( "Your account is locked.".equalsIgnoreCase(str) ) {
            return R.string.your_account_is_locked;
        }
        if ( "Your access token is invalid".equalsIgnoreCase(str) ) {
            return R.string.your_access_token_is_invalid;
        }
        if ( "Your access token has expired".equalsIgnoreCase(str) ) {
            return R.string.your_access_token_has_expired;
        }
        if ( "Your access token is not linked with this application".equalsIgnoreCase(str) ) {
            return R.string.your_access_token_is_not_linked_with_this_application;
        }
        if ( "Could not find application".equalsIgnoreCase(str) ) {
            return R.string.could_not_find_application;
        }
        if ( "Could not authenticate access code".equalsIgnoreCase(str) ) {
            return R.string.could_not_authenticate_access_code;
        }
        if ( "You must provide a client object".equalsIgnoreCase(str) ) {
            return R.string.you_must_provide_a_client_object;
        }
        if ( "Login fails.".equalsIgnoreCase(str) ) {
            return R.string.login_failed;
        }
        if ( "This application is not approved by Ayla Networks.".equalsIgnoreCase(str) ) {
            return R.string.this_application_is_not_approved_by_ayla;
        }
        if ( "The authentication token is invalid.".equalsIgnoreCase(str) ) {
            return R.string.the_authentication_token_is_invalid;
        }
        if ( "The refresh token is invalid.".equalsIgnoreCase(str) ) {
            return R.string.the_refresh_token_is_invalid;
        }
        if ( "Could not find this application for sign-up!".equalsIgnoreCase(str) ) {
            return R.string.could_not_find_this_application_for_sign_up;
        }
        if ( "You are forbidden to perform this operation".equalsIgnoreCase(str) ) {
            return R.string.you_are_forbidden_to_perform_this_operation;
        }
        if ( "Missing OEM parameter.".equalsIgnoreCase(str) ) {
            return R.string.missing_oem_parameter;
        }
        if ( "Unauthorized".equalsIgnoreCase(str) ) { // Just in case the default changes
            return R.string.unauthorized;
        }
        if ( "Invalid Json format".equalsIgnoreCase(str) ) {
            return R.string.invalid_json_format;
        }
        if ( "An unexpected error. Please try again later".equalsIgnoreCase(str) ) {
            return R.string.an_unexpected_error_please_try_again_later;
        }
        if ( "You are already signed in.".equalsIgnoreCase(str) ) {
            return R.string.you_are_already_sign_in;
        }
        if ( "You need to sign in or sign up before continuing.".equalsIgnoreCase(str) ) {
            return R.string.you_need_to_sign_in_or_sign_up_before_continuing;
        }
        if ( "You have to confirm your account before continuing.".equalsIgnoreCase(str) ) {
            return R.string.you_have_to_confirm_your_account_before_continuing;
        }
        if ( "Invalid email or password.".equalsIgnoreCase(str) ) {
            return R.string.invalid_email_password;
        }
        if ( "Invalid authentication token.".equalsIgnoreCase(str) ) {
            return R.string.invalid_authentication_token;
        }
        if ( "Your account was not activated yet.".equalsIgnoreCase(str) ) {
            return R.string.your_account_was_not_activated_yet;
        }
        if ( "Your session expired, please sign in again to continue.".equalsIgnoreCase(str) ) {
            return R.string.your_session_expired_please_sign_in_again_to_continue;
        }
        if ( "Your account has note been approved by an administrator yet.".equalsIgnoreCase(str) ) {
            //TODO: a Typo on server side, notify them. Correct the text after they fix it.
            return R.string.your_account_has_not_been_approved_by_an_administrator_yet;
        }
        if ( "The email address has not signed up for an account.".equalsIgnoreCase(str) ) {
            return R.string.the_email_address_has_not_signed_up_for_an_account;
        }
        //TODO: Add more matches when necessary, SVC-2335.
        return R.string.unauthorized;
    }// end of getLocalizedString

}
