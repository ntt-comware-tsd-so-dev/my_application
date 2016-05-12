
package com.aylanetworks.agilelink;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.aylanetworks.aylasdk.AylaNetworks;
import com.aylanetworks.aylasdk.AylaUser;
import com.aylanetworks.agilelink.framework.SSOManager;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Map;

/**
 * AgileLinkSSOManager.java
 * AgileLink Application Framework
 * Created by Raji Pillay on 10/12/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */



/**
 * Class derived from {@link SSOManager}
 * This is the default SSOManager for Agilelink. Implementers should create their own derived class.
 *
 */


public class AgilelinkSSOManager extends SSOManager{

    private final static String LOG_TAG = "AgileLinkSSOManager";
    public String identityProviderBaseUrl = "https://idp-emulation.ayladev.com/api/v1/";
    private SSOSessionParams sessionParams;


/* SSOManager singleton object*/

    private static AgilelinkSSOManager ssoLoginInstance;


/**
     * @return ssoLoginInstance Single instance of SSOManager
     */

    public static AgilelinkSSOManager getInstance() {
        if (ssoLoginInstance == null) {
            ssoLoginInstance = new AgilelinkSSOManager();
            ssoLoginInstance.sessionParams = new SSOSessionParams();
        }
        return ssoLoginInstance;
    }


/**
     * Inner class used to to store SSO session parameters.
     */

    private static class SSOSessionParams{


/**
         * User Id received from SSO Identity provider.
         */

        private String userId;

/**
         * Access token of the SSO Identity provider.
         */

        private String ipAccessToken;

/**
         * App ID of the SSO Identity provider.
         */

        public String ipAppId = "client-id";


/**
         * App secret of the SSO Identity provider.
         */

        public String ipAppSecret = "client-3431389";
    }




/**Method to login to SSO identity provider's service.
  * @param handle handler where results are returned
  * @param userName user email
  * @param password password
  *
*/

    public void login(final Handler handle, final String userName, final String password) {

        if(isConnected()){
            final String loginUrl = identityProviderBaseUrl + "sign_in?email=" + userName + "&password=" + password;

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    HttpURLConnection urlConn = null;
                    String response = null;
                    int responseCode = -1;
                    Message msg = new Message();
                    try {
                        URL url = new URL(loginUrl);
                        urlConn = (HttpURLConnection) url.openConnection();
                        urlConn.setConnectTimeout(10000);
                        urlConn.setRequestMethod("GET");
                        urlConn.setDoInput(true);

                        responseCode = urlConn.getResponseCode();
                        if(responseCode >= 200 && responseCode < 300){
                            InputStream iStream = urlConn.getInputStream();
                            response = convertStreamToString(iStream);
                            iStream.close();
                            urlConn.disconnect();
                            JSONObject responseJson = new JSONObject(response);
                            Log.d(LOG_TAG, "responseJson " + response);
                            getInstance().sessionParams.ipAccessToken = (String) responseJson.get("access_token");
                            getInstance().sessionParams.userId = (String) responseJson.get("uuid");
                            AylaUser.ssoLogin(handle, userName, password, getInstance().sessionParams.ipAccessToken, getInstance().sessionParams.ipAppId, getInstance().sessionParams.ipAppSecret);

                        } else{
                            InputStream iStream = urlConn.getErrorStream();
                            response = convertStreamToString(iStream);
                            iStream.close();
                            urlConn.disconnect();
                            handle.obtainMessage(AylaNetworks.AML_ERROR_FAIL, responseCode, 0,  response).sendToTarget();

                        }

                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                        handle.obtainMessage(AylaNetworks.AML_ERROR_FAIL, responseCode, 0, e.getLocalizedMessage()).sendToTarget();

                    } catch (IOException e) {
                        e.printStackTrace();
                        handle.obtainMessage(AylaNetworks.AML_ERROR_FAIL, responseCode, 0, e.getLocalizedMessage()).sendToTarget();

                    } catch (JSONException e) {
                        e.printStackTrace();
                        handle.obtainMessage(AylaNetworks.AML_ERROR_FAIL, responseCode, 0, e.getLocalizedMessage()).sendToTarget();

                    } finally {
                        if(urlConn != null){
                            urlConn.disconnect();
                        }
                    }


                }
            });

            thread.start();


        }
        else{
            handle.obtainMessage(AylaNetworks.AML_ERROR_FAIL, AylaNetworks.AML_ERROR_UNREACHABLE, 0, "Cloud service not reachable").sendToTarget();
        }

    }


/**
     * Method to update user info.
     * This method updates user info in SSO identity provider's service.
     * The SSO identity provider then invokes callback to update the user info in Ayla user service.
     * To be changed by app developer according to SSO identity provider's user service
     *
     * @param mHandle Results are returned to this handler
     * @param callParams contains updated values
     *

     */

    public void updateUserInfo(final Handler mHandle, Map<String, String> callParams) {

        if(isConnected()){
            Log.d(LOG_TAG, "userId "+getInstance().sessionParams.userId);
            final String url = identityProviderBaseUrl + "users/" + getInstance().sessionParams.userId;

            JSONObject userParams = new JSONObject();
            JSONObject errors = new JSONObject();
            String paramKey, paramValue;

            try {
                // test validity of objects
                paramKey = "email";
                paramValue = (String)callParams.get(paramKey);
                userParams.put(paramKey, paramValue);


                paramKey = "firstname";
                paramValue = (String)callParams.get(paramKey);
                if (paramValue == null) {
                    errors.put(paramKey, "can't be blank");
                } else {
                    if (paramValue.length() < 2) {
                        errors.put(paramKey, "is to short");
                    }
                }
                userParams.put(paramKey, paramValue);

                paramKey = "lastname";
                paramValue = (String)callParams.get(paramKey);
                if (paramValue == null) {
                    errors.put(paramKey, "can't be blank");
                } else {
                    if (paramValue.length() < 2) {
                        errors.put(paramKey, "is to short");
                    }
                }
                userParams.put(paramKey, paramValue);

                // Add all the parameters
                userParams.put("zip", (String)callParams.get("zip"));
                userParams.put("phone_country_code", (String)callParams.get("phone_country_code"));
                userParams.put("phone", (String)callParams.get("phone"));
                userParams.put("ayla_dev_kit_num", (String)callParams.get("aylaDevKitNum"));
                userParams.put("company", (String)callParams.get("company"));
                userParams.put("street", (String)callParams.get("street"));
                userParams.put("city", (String)callParams.get("city"));
                userParams.put("state", (String) callParams.get("state"));
                userParams.put("nickname", "Nickname");


            } catch (Exception e) {
                e.printStackTrace();
            }

            final String updatedUserParam = userParams.toString();
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {

                    HttpURLConnection urlConn = null;
                    Message msg = new Message();
                    String response = null;
                    int responseCode = -1;
                    try {
                        URL updateUrl = new URL(url);
                        urlConn = (HttpURLConnection) updateUrl.openConnection();
                        urlConn.setConnectTimeout(10000);
                        urlConn.setRequestMethod("PUT");
                        urlConn.setDoInput(true);
                        urlConn.setDoOutput(true);
                        urlConn.setRequestProperty("Content-Type", "application/json");
                        urlConn.setRequestProperty("APP-ID",getInstance().sessionParams.ipAppId);
                        urlConn.setRequestProperty("APP-SECRET", getInstance().sessionParams.ipAppSecret);
                        urlConn.setRequestProperty("AYLA-URL", AylaSystemUtils.userServiceBaseURL() );
                        OutputStream oStream = urlConn.getOutputStream();
                        oStream.write(updatedUserParam.getBytes());
                        oStream.close();

                        responseCode = urlConn.getResponseCode();
                        if(responseCode >= 200 && responseCode < 300){
                            InputStream iStream = urlConn.getInputStream();
                            response = convertStreamToString(iStream);
                            iStream.close();
                            urlConn.disconnect();
                            Log.d(LOG_TAG, "updateUser response " + response);
                            mHandle.obtainMessage(AylaNetworks.AML_ERROR_OK, responseCode, 0, response).sendToTarget();

                        }
                        else{
                            InputStream iStream = urlConn.getErrorStream();
                            response = convertStreamToString(iStream);
                            iStream.close();
                            urlConn.disconnect();
                            Log.d(LOG_TAG, "updateUser error " + response);
                            mHandle.obtainMessage(AylaNetworks.AML_ERROR_FAIL, responseCode, 0, response).sendToTarget();

                        }

                    } catch (ProtocolException e) {
                        e.printStackTrace();
                        mHandle.obtainMessage(AylaNetworks.AML_ERROR_FAIL, responseCode, 0, e.getLocalizedMessage()).sendToTarget();
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                        mHandle.obtainMessage(AylaNetworks.AML_ERROR_FAIL, responseCode, 0, e.getLocalizedMessage()).sendToTarget();
                    } catch (IOException e) {
                        e.printStackTrace();
                        mHandle.obtainMessage(AylaNetworks.AML_ERROR_FAIL, responseCode, 0, e.getLocalizedMessage()).sendToTarget();
                    } finally{
                        if(urlConn != null){
                            urlConn.disconnect();
                        }
                    }
                }
            });
            thread.start();
        } else {
            mHandle.obtainMessage(AylaNetworks.AML_ERROR_FAIL, AylaNetworks.AML_ERROR_UNREACHABLE, 0, "Cloud service not reachable").sendToTarget();
        }


    }


/**
     * Method to delete user.
     * This method will delete user from SSO identity provider's service.
     * The SSO identity provider then invokes callback to delete user from Ayla user service.
     * To be changed by app developer according to SSO identity provider's user service.
     * @param handler Results are returned to this handler
     */

    public void deleteUser(Handler handler){
        if(isConnected()){
            final String url = identityProviderBaseUrl + "/users/" + getInstance().sessionParams.userId;
            final Handler mHandle = handler;

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    HttpURLConnection urlConn = null;
                    int responseCode = -1;
                    String response = null;
                    try {
                        URL deleteUrl = new URL(url);
                        urlConn = (HttpURLConnection) deleteUrl.openConnection();
                        urlConn.setRequestMethod("DELETE");
                        urlConn.setRequestProperty("Content-Type", "application/json");
                        urlConn.setRequestProperty("APP-ID", getInstance().sessionParams.ipAppId);
                        urlConn.setRequestProperty("APP-SECRET", getInstance().sessionParams.ipAppSecret);
                        urlConn.setRequestProperty("AYLA-URL", AylaSystemUtils.userServiceBaseURL());
                        urlConn.setDoInput(true);

                        responseCode = urlConn.getResponseCode();
                        if(responseCode >= 200 && responseCode < 300){
                            urlConn.disconnect();
                            mHandle.obtainMessage(AylaNetworks.AML_ERROR_OK, responseCode, 0, "Success").sendToTarget();
                        } else{
                            InputStream iStream = urlConn.getErrorStream();
                            response = convertStreamToString(iStream);
                            iStream.close();
                            urlConn.disconnect();
                            mHandle.obtainMessage(AylaNetworks.AML_ERROR_FAIL, responseCode, 0, response).sendToTarget();

                        }



                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                        mHandle.obtainMessage(AylaNetworks.AML_ERROR_FAIL, responseCode, 0, e.getLocalizedMessage()).sendToTarget();
                    } catch (IOException e) {
                        e.printStackTrace();
                        mHandle.obtainMessage(AylaNetworks.AML_ERROR_FAIL, responseCode, 0, e.getLocalizedMessage()).sendToTarget();
                    } finally {
                        if(urlConn != null){
                            urlConn.disconnect();
                        }
                    }

                }
            });
            thread.start();
        } else{
            handler.obtainMessage(AylaNetworks.AML_ERROR_FAIL, AylaNetworks.AML_ERROR_UNREACHABLE, 0, "Cloud service not reachable").sendToTarget();
        }


    }


    private static String convertStreamToString(InputStream is) {

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                String eMsg = (e.getLocalizedMessage() == null) ? e.toString() : e.getLocalizedMessage();
                AylaSystemUtils.saveToLog("%s, %s, %s, %s:%s %s", "E", LOG_TAG, "Error", "eMsg", eMsg, "commit.convertStreamToString");
            }
        }
        return sb.toString();
    }

    private static boolean isConnected(){
        ConnectivityManager connectivityManager = (ConnectivityManager) AgileLinkApplication.getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if(info == null){
            return false;
        }
        else{
            return true;
        }
    }

}


