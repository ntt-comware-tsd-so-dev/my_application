package com.aylanetworks.agilelink.fragments;


import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.Response;
import com.aylanetworks.agilelink.framework.AMAPCore;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.DeviceNotificationHelper;
import com.aylanetworks.agilelink.framework.Logger;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;
import com.aylanetworks.aylasdk.setup.AylaRegistration;
import com.aylanetworks.aylasdk.setup.AylaRegistrationCandidate;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Emmanuel Luna on 06/16/15.
 */
public class ConnectGatewayFragment extends Fragment implements View.OnClickListener {

    private static final String LOG_TAG = "ConnectGatewayFragment";

    private Button btnGateway;
    private View mView;

    private Timer timer;
    private TimerTask timerTask;

    private AylaRegistration _aylaRegistration;

    private final Handler uiHandler = new Handler();

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_connect_gateway, container, false);
        btnGateway = (Button)mView.findViewById(R.id.register_btn);
        btnGateway.setOnClickListener(this);
        return mView;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.register_btn:
                registerButtonClick();
                break;
        }
    }

    private void registerButtonClick() {
        Log.i(LOG_TAG, "rn: registerNewGateway");
        MainActivity.getInstance().showWaitDialog(null, null);
        AylaRegistrationCandidate candidate = new AylaRegistrationCandidate();
        candidate.setRegistrationType(AylaDevice.RegistrationType.ButtonPush);
        //Gateway is always push button
        registerNewDevice(candidate);
    }

    private void registerNewDevice(AylaRegistrationCandidate candidate) {
        MainActivity.getInstance().showWaitDialog(R.string.registering_device_title, R.string.registering_device_body);
        Log.i(LOG_TAG, "rn: Calling registerNewDevice...");

        if (_aylaRegistration == null) {
            _aylaRegistration = new AylaRegistration(AMAPCore.sharedInstance().getDeviceManager());
        }
        _aylaRegistration.registerCandidate(candidate, new Response.Listener<AylaDevice>() {
                    @Override
                    public void onResponse(AylaDevice device) {
                        MainActivity.getInstance().showWaitDialog(R.string.updating_notifications_title, R.string.updating_notifications_body);
                        // Now update the device notifications
                        DeviceNotificationHelper helper = new DeviceNotificationHelper(device);
                        helper.initializeNewDeviceNotifications(
                                new DeviceNotificationHelper.DeviceNotificationHelperListener() {
                                    @Override
                                    public void newDeviceUpdated
                                            (AylaDevice device,
                                             AylaError error) {
                                        Logger.logInfo(LOG_TAG, "rn: newDeviceUpdated [" + device + "]");
                                        MainActivity mainActivity = MainActivity.getInstance();
                                        mainActivity.dismissWaitDialog();
                                        int msgId = (error == null ? R.string.registration_success :
                                                R.string.registration_success_notification_fail);
                                        Toast.makeText(mainActivity, msgId, Toast.LENGTH_LONG).show();
                                        if (error == null) {
                                            // Go to the Gateway display
                                            Log.v(LOG_TAG, "rn: registration successful. select gateways.");
                                            MainActivity.getInstance().onSelectMenuItemById(R.id.action_gateways);
                                        } else {
                                            Log.w(LOG_TAG, "rn: registration unsuccessful.");
                                        }
                                    }
                                });
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        Log.w(LOG_TAG, "rn: could not register device.");
                        Toast.makeText(getActivity(), R.string.registration_failure,
                                Toast.LENGTH_LONG).show();
                        exitSetup();
                    }
                });
    }


    private static void exitSetup() {
    }

    @Override
    public void onResume() {
        super.onResume();
        startTickTimer();
    }

    @Override
    public void onStop() {
        // TODO Auto-generated method stub
        super.onStop();
        if(timer != null){
            timer.cancel();
            timer =   null;
        }
    }

    private void startTickTimer(){
        timer = new Timer();
        initializeTimer();
        timer.schedule(timerTask, 1000/24,1000/24);
    }

    private void initializeTimer(){
        frame =0;
        timerTask = new TimerTask() {

            @Override
            public void run() {
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        updateLEDs();
                    }
                });
            }
        };
    }

    private int frame;

    private void updateLEDs(){
        Activity activity = getActivity();
        frame = (++frame)%36;
        if(frame%4 == 0){
            if(activity.findViewById(R.id.redled2) != null){
                activity.findViewById(R.id.redled2).setVisibility(activity.findViewById(R.id.redled2).getVisibility() == View.VISIBLE?View.GONE:View.VISIBLE);
            }
        }

        if(frame%24 == 0 || frame%30 == 0 || frame%33 == 0 || frame%36 == 0){
            if(activity.findViewById(R.id.greenled2) != null){
                activity.findViewById(R.id.greenled2).setVisibility(activity.findViewById(R.id.greenled2).getVisibility() == View.VISIBLE?View.GONE:View.VISIBLE);
            }
        }
    }
}
