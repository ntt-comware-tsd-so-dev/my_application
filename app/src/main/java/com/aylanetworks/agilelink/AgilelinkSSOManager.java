package com.aylanetworks.agilelink;

import android.content.SharedPreferences;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.aylanetworks.agilelink.framework.AMAPCore;
import com.aylanetworks.agilelink.framework.IdentityProviderAuth;
import com.aylanetworks.agilelink.framework.SSOManager;
import com.aylanetworks.aylasdk.AylaAPIRequest;
import com.aylanetworks.aylasdk.AylaJsonRequest;
import com.aylanetworks.aylasdk.AylaLog;
import com.aylanetworks.aylasdk.AylaLoginManager;
import com.aylanetworks.aylasdk.AylaNetworks;
import com.aylanetworks.aylasdk.AylaSessionManager;
import com.aylanetworks.aylasdk.AylaSystemSettings;
import com.aylanetworks.aylasdk.AylaUser;
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
 * AMAP_Android
 * <p>
 * Copyright 2016 Ayla Networks, all rights reserved
 */


/**
 * AgilelinkSSOManager class implements methods in {@link SSOManager} class to use Ayla's test
 * identity provider service.
 */
public class AgilelinkSSOManager extends SSOManager {
    private final static String LOG_TAG = "AgileLinkSSOManager";
    public String _identityProviderBaseUrl = "https://idp-emulation.ayladev.com/api/v1/";
    public static final String USER_ID_KEY = "UUID";

    /**
     * Method to login to identity provider service.
     * @param username Valid username
     * @param password Valid password
     * @param successListener listener to receive result on successful login
     * @param errorListener listener to receive error information in case of failure
     */
    @Override
    public AylaAPIRequest login(String username, String password, Response.Listener<IdentityProviderAuth> successListener,
                      ErrorListener errorListener) {
        AylaLog.d(LOG_TAG, " Starting SSO login to provider");
        final String loginUrl = _identityProviderBaseUrl + "sign_in?email=" + username +
                "&password=" + password;
        AylaSessionManager sessionManager = AMAPCore.sharedInstance().getSessionManager();
        AylaAPIRequest<IdentityProviderAuth> request = new AylaAPIRequest<>(Request.Method.GET,
                loginUrl, null, IdentityProviderAuth.class, sessionManager, successListener,
                errorListener);
        AylaLoginManager loginManager = AylaNetworks.sharedInstance().getLoginManager();
        loginManager.sendUserServiceRequest(request);
        return request;
    }

    /**
     * Method to update the user information in the identitiy provider service.
     * @param updatedUser Ayla user containing new values to be updated.
     * @param successListener listener to receive result on successful login
     * @param errorListener listener to receive error information in case of failure
     */
    @Override
    public AylaAPIRequest updateUserInfo(final AylaUser updatedUser, final Response.Listener<AylaUser>
            successListener, final ErrorListener errorListener) {

        String uuid = getUUid();
        if(uuid == null){
            errorListener.onErrorResponse(new PreconditionError("No user id"));
            return null;
        }
        AylaSessionManager sessionManager = AMAPCore.sharedInstance().getSessionManager();
        if(sessionManager == null){
            errorListener.onErrorResponse(new PreconditionError("No valid session"));
            return null;
        }
        final String url = _identityProviderBaseUrl + "users/" +uuid;

        String bodyJson = AylaNetworks.sharedInstance().getGson().toJson(
                getAylaSSOUser(updatedUser), AylaSSOUser.class);
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
    public AylaAPIRequest deleteUser(Response.Listener<AylaAPIRequest.EmptyResponse> successListener, ErrorListener errorListener) {

        AylaSessionManager sessionManager = AMAPCore.sharedInstance().getSessionManager();
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
    public AylaAPIRequest signOut(Response.Listener<AylaAPIRequest.EmptyResponse> successListener,
                        ErrorListener errorListener) {
        AylaSessionManager sessionManager = AMAPCore.sharedInstance().getSessionManager();
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
