package com.aylanetworks.agilelink.fragments;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.Response;
import com.aylanetworks.agilelink.ErrorUtils;
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

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Emmanuel Luna on 06/16/15.
 */
public class ConnectGatewayFragment extends Fragment implements View.OnClickListener {

    private static final String LOG_TAG = "ConnectGatewayFragment";
    private static final int REQUEST_LOCATION = 2;

    private Timer timer;
    private TimerTask timerTask;

    private AylaRegistration _aylaRegistration;

    private final Handler uiHandler = new Handler();

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_connect_gateway, container, false);
        Button btnGateway = (Button) view.findViewById(R.id.register_btn);
        btnGateway.setOnClickListener(this);
        return view;
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
        MainActivity.getInstance().showWaitDialog("Please wait...", "Registering gateway");
        //Gateway is always push button
        fetchCandidateAndRegister();
    }

    private void fetchCandidateAndRegister() {
        MainActivity.getInstance().showWaitDialog(R.string.registering_device_title, R.string.registering_device_body);

        if (_aylaRegistration == null) {
            _aylaRegistration = new AylaRegistration(AMAPCore.sharedInstance().getDeviceManager());
        }

        AylaDevice.RegistrationType regType = AylaDevice.RegistrationType.ButtonPush;
        _aylaRegistration.fetchCandidate(null, regType,
                new Response.Listener<AylaRegistrationCandidate>() {
                    @Override
                    public void onResponse(AylaRegistrationCandidate candidate) {
                        registerNewDevice(candidate);
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        MainActivity.getInstance().dismissWaitDialog();

                        Toast.makeText(getActivity(),
                                ErrorUtils.getUserMessage(getContext(), error, R.string.error_fetch_candidates),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void registerNewDevice(AylaRegistrationCandidate candidate) {
        Log.i(LOG_TAG, "rn: Calling registerNewDevice...");

        // This is optional. Add location information to send latitude and longitude during
        // registration of device.
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestScanPermissions();
        } else {
            LocationManager locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_COARSE);
            Location currentLocation;
            List<String> locationProviders = locationManager.getAllProviders();
            for (String provider: locationProviders) {
                currentLocation = locationManager.getLastKnownLocation(provider);
                if (currentLocation != null) {
                    candidate.setLatitude(String.valueOf(currentLocation.getLatitude()));
                    candidate.setLongitude(String.valueOf(currentLocation.getLongitude()));
                    break;
                }
            }
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
                                    public void newDeviceUpdated(AylaDevice device, AylaError error) {
                                        Logger.logInfo(LOG_TAG, "rn: newDeviceUpdated [" + device + "]");
                                        MainActivity.getInstance().dismissWaitDialog();

                                        int msgId = (error == null ? R.string.registration_success :
                                                R.string.registration_success_notification_fail);
                                        Toast.makeText(getContext(), msgId, Toast.LENGTH_LONG).show();

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
                        MainActivity.getInstance().dismissWaitDialog();

                        Toast.makeText(getActivity(),
                                ErrorUtils.getUserMessage(getActivity(), error, R.string.registration_failure),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        startTickTimer();
    }

    @Override
    public void onStop() {
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
                        updateLEDs();
                    }
                });
            }
        };
    }

    private int frame;

    private void updateLEDs(){
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

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

    /*
   * Scan needs location permissions. This method requests Location permission
    */
    private void requestScanPermissions(){
        ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);
    }
}
