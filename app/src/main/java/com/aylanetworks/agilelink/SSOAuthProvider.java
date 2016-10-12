package com.aylanetworks.agilelink;

/**
 * AMAP_Android
 * <p>
 * Copyright 2016 Ayla Networks, all rights reserved
 */

import android.content.SharedPreferences;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.aylanetworks.agilelink.framework.AMAPCore;
import com.aylanetworks.agilelink.framework.IdentityProviderAuth;
import com.aylanetworks.aylasdk.AylaAPIRequest;
import com.aylanetworks.aylasdk.AylaJsonRequest;
import com.aylanetworks.aylasdk.AylaLog;
import com.aylanetworks.aylasdk.AylaLoginManager;
import com.aylanetworks.aylasdk.AylaNetworks;
import com.aylanetworks.aylasdk.AylaSessionManager;
import com.aylanetworks.aylasdk.AylaSystemSettings;
import com.aylanetworks.aylasdk.AylaUser;
import com.aylanetworks.aylasdk.auth.AylaAuthProvider;
import com.aylanetworks.aylasdk.auth.AylaAuthorization;
import com.aylanetworks.aylasdk.error.AuthError;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;
import com.aylanetworks.aylasdk.error.PreconditionError;
import com.aylanetworks.aylasdk.util.ServiceUrls;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * SSOAuthProvider that provides authorization to the Ayla network using the access token
 * provided by external identity provider for Single Sign On.
 */
public class SSOAuthProvider implements AylaAuthProvider {
    private String _token;
    private final static String LOG_TAG = "AgileLinkSSOManager";
    public String _identityProviderBaseUrl = "https://idp-emulation.ayladev.com/api/v1/";
    public static final String USER_ID_KEY = "UUID";

    public void setToken(String token) {
        this._token = token;
    }

    @Override
    public void authenticate(final AuthProviderListener listener) {
        AylaLoginManager loginManager = AylaNetworks.sharedInstance().getLoginManager();
        String url = loginManager.userServiceUrl("api/v1/token_sign_in.json");

        AylaSystemSettings settings = AylaNetworks.sharedInstance().getSystemSettings();

        // Construct a JSON object to contain the parameters.
        JSONObject userParam = new JSONObject();
        try {
            userParam.put("token", _token);
            userParam.put("app_id", settings.appId);
            userParam.put("app_secret", settings.appSecret);
        } catch (JSONException e) {
            listener.didFailAuthentication(new AuthError("JSONException in signIn()", e));
            return;
        }

        String bodyText = userParam.toString();
        AylaAPIRequest<AylaAuthorization> request = new AylaJsonRequest<>(
                Request.Method.POST,
                url,
                bodyText,
                null,
                AylaAuthorization.class,
                null, // No session manager exists until we are logged in!
                new Response.Listener<AylaAuthorization>() {
                    @Override
                    public void onResponse(AylaAuthorization response) {
                        listener.didAuthenticate(response, false);
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        listener.didFailAuthentication(error);
                    }
                });

        loginManager.sendUserServiceRequest(request);
    }

    @Override
    public void authenticate(AuthProviderListener listener, String sessionName) {
        authenticate(listener);
    }

    @Override
    public AylaAPIRequest updateUserProfile(AylaSessionManager sessionManager, AylaUser user,
                                            final Response.Listener<AylaUser> successListener,
                                            ErrorListener errorListener) {
        String uuid = getUUid();
        if(uuid == null){
            errorListener.onErrorResponse(new PreconditionError("No user id"));
            return null;
        }
        if(sessionManager == null){
            errorListener.onErrorResponse(new PreconditionError("No valid session"));
            return null;
        }
        final String url = _identityProviderBaseUrl + "users/" +uuid;

        String bodyJson = AylaNetworks.sharedInstance().getGson().toJson(
                getAylaSSOUser(user), AylaSSOUser.class);
        AylaJsonRequest<AylaSSOUser> request = new AylaJsonRequest<AylaSSOUser>(Request.Method.PUT, url,
                bodyJson, getSSOHeaders(), AylaSSOUser.class, sessionManager,
                new Response.Listener<AylaSSOUser>() {
                    @Override
                    public void onResponse(AylaSSOUser response) {
                        successListener.onResponse(response.getAylaUser());
                    }
                }, errorListener){
            @Override
            protected Response parseNetworkResponse(NetworkResponse response) {
                String responseString = new String(response.data);
                AylaSSOUser user = null;
                AylaLog.d(LOG_TAG, "Response from identity provider service "+responseString);
                try {
                    JSONObject jsonObj = new JSONObject(responseString);
                    JSONObject responseObj = jsonObj.getJSONObject("response");
                    JSONObject userObj = responseObj.getJSONObject("user");
                    Gson gson = AylaNetworks.sharedInstance().getGson();
                    user = gson.fromJson(userObj.toString(), AylaSSOUser.class);
                    return  Response.success(user, HttpHeaderParser.parseCacheHeaders(response));
                } catch (JSONException e) {
                    e.printStackTrace();
                    return Response.error(new ParseError(response));
                }
            }
        };
        sessionManager.sendUserServiceRequest(request);
        return  request;
    }

    @Override
    public AylaAPIRequest deleteUser(AylaSessionManager sessionManager, Response.Listener<AylaAPIRequest.EmptyResponse> successListener, ErrorListener errorListener) {
        if(sessionManager == null){
            errorListener.onErrorResponse(new PreconditionError("No valid session"));
            return null;
        }
        String uuid = getUUid();
        if(uuid == null){
            errorListener.onErrorResponse(new PreconditionError("No user id"));
            return null;
        }
        String url = _identityProviderBaseUrl + "users/" + uuid;

        AylaAPIRequest<AylaAPIRequest.EmptyResponse> request = new AylaAPIRequest<>(
                Request.Method.DELETE, url, getSSOHeaders(), AylaAPIRequest.EmptyResponse.class,
                sessionManager, successListener, errorListener );
        sessionManager.sendUserServiceRequest(request);
        return  request;
    }

    @Override
    public AylaAPIRequest signout(AylaSessionManager sessionManager, Response.Listener<AylaAPIRequest.EmptyResponse> successListener, ErrorListener errorListener) {
        if(sessionManager == null){
            errorListener.onErrorResponse(new PreconditionError("No valid session"));
            return null;
        }
        String uuid = getUUid();
        if(uuid == null){
            errorListener.onErrorResponse(new PreconditionError("No user id"));
            return null;
        }
        JSONObject jsonObj = new JSONObject();
        try {
            jsonObj.put("sign_out", "true");
        } catch (JSONException e) {
            errorListener.onErrorResponse(new PreconditionError("JsonException while creating " +
                    "json body"));
            return null;
        }

        String url = _identityProviderBaseUrl + "users/" + uuid ;
        AylaJsonRequest<AylaAPIRequest.EmptyResponse> request = new AylaJsonRequest<>(Request
                .Method.POST, url, jsonObj.toString(), getSSOHeaders(), AylaAPIRequest
                .EmptyResponse.class, sessionManager, successListener, errorListener);
        sessionManager.sendUserServiceRequest(request);
        return request;
    }

    public AylaAPIRequest ssoLogin(String username, String password, final Response
            .Listener<IdentityProviderAuth> successListener, ErrorListener errorListener){
        AylaLog.d(LOG_TAG, " Starting SSO login to provider");
        final String loginUrl = _identityProviderBaseUrl + "sign_in?email=" + username +
                "&password=" + password;
        AylaSessionManager sessionManager = AMAPCore.sharedInstance().getSessionManager();
        AylaAPIRequest<IdentityProviderAuth> request = new AylaAPIRequest<>(Request.Method.GET,
                loginUrl, null, IdentityProviderAuth.class, sessionManager, new Response.Listener<IdentityProviderAuth>() {
            @Override
            public void onResponse(IdentityProviderAuth response) {
                // uuId for all identity provider APIs
                SharedPreferences prefs = AgileLinkApplication.getSharedPreferences();
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(SSOAuthProvider.USER_ID_KEY, response.getUuid());
                editor.commit();
                _token = response.getAccessToken();
                successListener.onResponse(response);
            }
        },
                errorListener);
        AylaLoginManager loginManager = AylaNetworks.sharedInstance().getLoginManager();
        loginManager.sendUserServiceRequest(request);
        return request;
    }


    public class AylaSSOUser{
        @Expose
        private String uuid;
        @Expose
        private String email;
        @Expose
        private String phone;
        @Expose
        private String firstname;
        @Expose
        private String lastname;
        @Expose
        private String nickname;

        public AylaUser getAylaUser(){
            AylaUser aylaUser = new AylaUser();
            aylaUser.setFirstname(firstname);
            aylaUser.setLastname(lastname);
            aylaUser.setPhone(phone);
            aylaUser.setEmail(email);

            return aylaUser;
        }
    }

    private Map<String, String> getSSOHeaders(){
        Map<String, String> headers = new HashMap<>(3);
        headers.put("APP-ID", AMAPCore.sharedInstance().getSessionParameters().appId);
        headers.put("APP-SECRET", AMAPCore.sharedInstance().getSessionParameters().appSecret);
        headers.put("AYLA-URL", ServiceUrls.getBaseServiceURL(ServiceUrls.CloudService.User,
                AylaSystemSettings.ServiceType.Development, AylaSystemSettings.ServiceLocation.USA));
        return headers;
    }

    private AylaSSOUser getAylaSSOUser(AylaUser user){
        AylaSSOUser ssoUser = new AylaSSOUser();
        ssoUser.firstname = user.getFirstname();
        ssoUser.lastname = user.getLastname();
        ssoUser.email = user.getEmail();
        ssoUser.phone = user.getPhone();
        return ssoUser;
    }

    private String getUUid(){
        SharedPreferences prefs = AgileLinkApplication.getSharedPreferences();
        return prefs.getString(USER_ID_KEY, null);

    }

}



