package com.aylanetworks.agilelink;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
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

import com.android.volley.Response;
import com.aylanetworks.agilelink.framework.AMAPCore;
import com.aylanetworks.aylasdk.AylaAPIRequest;
import com.aylanetworks.aylasdk.AylaEmailTemplate;
import com.aylanetworks.aylasdk.AylaNetworks;
import com.aylanetworks.aylasdk.AylaSessionManager;
import com.aylanetworks.aylasdk.AylaSystemSettings;
import com.aylanetworks.aylasdk.AylaUser;
import com.aylanetworks.agilelink.fragments.ResetPasswordDialog;
import com.aylanetworks.agilelink.fragments.SignUpDialog;
import com.aylanetworks.aylasdk.auth.AylaAuthorization;
import com.aylanetworks.aylasdk.auth.AylaOAuthProvider;
import com.aylanetworks.aylasdk.auth.CachedAuthProvider;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;

/*
 * SignInActivity.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 1/30/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class SignInActivity extends FragmentActivity implements SignUpDialog.SignUpListener, AylaSessionManager.SessionManagerListener {
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

    private final static String _serviceTypes[] = {"Dynamic", "Field", "Development", "Staging", "Demo"};

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
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_large_text, _serviceTypes);
        _serviceTypeSpinner.setAdapter(adapter);
        _serviceTypeSpinner.setSelection(AylaNetworks.sharedInstance().getSystemSettings().serviceType.ordinal());

        // We need to do this in a runnable so we don't get the first onItemSelected call from
        // the above call to setSelection.
        _serviceTypeSpinner.postDelayed(new Runnable() {
            @Override
            public void run() {
                _serviceTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if (position == AylaSystemSettings.ServiceType.Field.ordinal()) {
                            SessionManager.getInstance().setServiceType(AylaNetworks.AML_PRODUCTION_SERVICE);
                            Toast.makeText(MainActivity.getInstance(), "Production Service", Toast.LENGTH_LONG).show();
                        } else if (position == AylaSystemSettings.ServiceType.Staging.ordinal()) {
                            SessionManager.getInstance().setServiceType(AylaNetworks.AML_STAGING_SERVICE);
                            Toast.makeText(MainActivity.getInstance(), "Staging Service", Toast.LENGTH_LONG).show();
                        } else {
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
                    AMAPCore.sharedInstance().startSession(_username.getText().toString(), _password.getText().toString(),
                            new Response.Listener<AylaAuthorization>() {
                                @Override
                                public void onResponse(AylaAuthorization response) {
                                    Toast.makeText(getContext(), "Login successful", Toast.LENGTH_SHORT).show();

                                    // Cache the authorization
                                    CachedAuthProvider.cacheAuthorization(SignInActivity.this, response);

                                    finish();
                                }
                            },
                            new ErrorListener() {
                                @Override
                                public void onErrorResponse(AylaError error) {
                                    Toast.makeText(MainActivity.getInstance(),
                                            ErrorUtils.getUserMessage(getContext(), error, "Error starting session"),
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                }
            }
        );

        _facebookLoginButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    oAuthSignIn(AylaOAuthProvider.AccountType.facebook);
                }
        });

        _googleLoginButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    oAuthSignIn(AylaOAuthProvider.AccountType.google);
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

        AMAPCore.sharedInstance().getSessionManager().addListener(this);
    }

    @Override
    protected void onDestroy() {
        Log.d(LOG_TAG, "nod: onDestroy");
        if ( _progressDialog != null ) {
            _progressDialog.dismiss();
        }
        AMAPCore.sharedInstance().getSessionManager().removeListener(this);
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

    private void oAuthSignIn(AylaOAuthProvider.AccountType type) {
        _webView.setVisibility(View.VISIBLE);
        CookieManager.getInstance().removeAllCookie();

        // Clear out any previous contents of the webview
        String webViewEmptyHTML = this.getResources().getString(R.string.oauth_empty_html);
        webViewEmptyHTML = webViewEmptyHTML.replace("[[PROVIDER]]", getString(type.equals(AylaOAuthProvider.AccountType.google) ? R.string.google : R.string.facebook));
        _webView.loadDataWithBaseURL("", webViewEmptyHTML, "text/html", "UTF-8", "");
        _webView.bringToFront();

        _loginButton.setVisibility(View.INVISIBLE);
        AMAPCore.SessionParameters params = AMAPCore.sharedInstance().getSessionParameters();

        AylaOAuthProvider aylaOAuthProvider = new AylaOAuthProvider(type, _webView);
        AylaNetworks.sharedInstance().getLoginManager().signIn(aylaOAuthProvider,
                params.sessionName,
                new Response.Listener<AylaAuthorization>() {
                    @Override
                    public void onResponse(AylaAuthorization response) {
                        // Cache the authorization
                        CachedAuthProvider.cacheAuthorization(SignInActivity.this, response);

                        finish();
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        Toast.makeText(getContext(),
                                ErrorUtils.getUserMessage(MainActivity.getInstance(), error, R.string.login_failed),
                                Toast.LENGTH_LONG).show();
                    }
                });
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

                        AMAPCore.SessionParameters sessionParams = AMAPCore.sharedInstance().getSessionParameters();
                        AylaEmailTemplate emailTemplate = new AylaEmailTemplate();
                        emailTemplate.setEmailTemplateId(sessionParams.registrationEmailTemplateId);
                        emailTemplate.setEmailBodyHtml(sessionParams.registrationEmailBodyHTML);
                        emailTemplate.setEmailSubject(sessionParams.registrationEmailSubject);

                        AylaNetworks.sharedInstance().getLoginManager().resendConfirmationEmail(emailEditText.getText().toString(),
                                emailTemplate,
                                new Response.Listener<AylaAPIRequest.EmptyResponse>() {
                                    @Override
                                    public void onResponse(AylaAPIRequest.EmptyResponse response) {
                                        Toast.makeText(SignInActivity.this, R.string.email_confirmation_sent, Toast.LENGTH_LONG).show();
                                    }
                                },
                                new ErrorListener() {
                                    @Override
                                    public void onErrorResponse(AylaError error) {
                                        Toast.makeText(MainActivity.getInstance(),
                                                ErrorUtils.getUserMessage(getContext(), error, R.string.error_account_confirm_failed),
                                                Toast.LENGTH_LONG).show();
                                    }
                                });
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

                        AMAPCore.SessionParameters sessionParams = AMAPCore.sharedInstance().getSessionParameters();
                        AylaEmailTemplate emailTemplate = new AylaEmailTemplate();
                        emailTemplate.setEmailTemplateId(sessionParams.resetPasswordEmailTemplateId);
                        emailTemplate.setEmailBodyHtml(sessionParams.resetPasswordEmailBodyHTML);
                        emailTemplate.setEmailSubject(sessionParams.resetPasswordEmailSubject);

                        AylaNetworks.sharedInstance().getLoginManager().requestPasswordReset(email, emailTemplate,
                                new Response.Listener<AylaAPIRequest.EmptyResponse>() {
                                    @Override
                                    public void onResponse(AylaAPIRequest.EmptyResponse response) {
                                        Toast.makeText(SignInActivity.this, R.string.password_reset_sent, Toast.LENGTH_LONG).show();
                                        _username.setText(emailEditText.getText().toString());
                                    }
                                },
                                new ErrorListener() {
                                    @Override
                                    public void onErrorResponse(AylaError error) {
                                        Toast.makeText(MainActivity.getInstance(),
                                                ErrorUtils.getUserMessage(getContext(), error, R.string.error_password_reset_failed),
                                                Toast.LENGTH_LONG).show();
                                    }
                                });
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
        _username.setText(newUser.getEmail());
        _password.setText(newUser.getPassword());
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

    private void handleUserSignupToken(String token) {
        Log.d(LOG_TAG, "nod: handleUserSignupToken: " + token);
        AylaNetworks.sharedInstance().getLoginManager().confirmSignUp(token,
                new Response.Listener<AylaAPIRequest.EmptyResponse>() {
                    @Override
                    public void onResponse(AylaAPIRequest.EmptyResponse response) {
                        /**
                        AylaUser aylaUser = AylaSystemUtils.gson.fromJson(jsonResults, AylaUser.class);
                        AylaSystemUtils.saveSetting(SessionManager.AYLA_SETTING_CURRENT_USER, jsonResults);
                        String toastMessage = getString(R.string.welcome_new_account);
                        Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
                        _username.setText(aylaUser.getEmail());
                        _password.setText(aylaUser.getPassword());
                        Log.d(LOG_TAG, "nod: SignUpConfirmationHandler set user & password");
                         */
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        AlertDialog.Builder ad = new AlertDialog.Builder(getContext());
                        ad.setIcon(R.drawable.ic_launcher);
                        ad.setTitle(R.string.error_sign_up_title);
                        ad.setMessage(ErrorUtils.getUserMessage(getContext(), error, R.string.error_account_confirm_failed));
                        ad.setPositiveButton(android.R.string.ok, null);
                        ad.show();
                    }
                });
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
    public void sessionClosed(String sessionName, AylaError error) {

    }

    @Override
    public void authorizationRefreshed(String sessionName, AylaAuthorization authorization) {

    }

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
