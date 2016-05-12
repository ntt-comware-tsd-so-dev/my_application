package com.aylanetworks.agilelink.fragments;


import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaSetup;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.aaml.AylaUser;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.DeviceNotificationHelper;
import com.aylanetworks.agilelink.framework.Logger;
import com.aylanetworks.agilelink.framework.MenuHandler;
import com.aylanetworks.agilelink.framework.SessionManager;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Emmanuel Luna on 06/16/15.
 */
public class ConnectGatewayFragment extends Fragment implements View.OnClickListener {

    private static final String LOG_TAG = "ConnectGatewayFragment";
    private static final int REQUEST_LOCATION = 2;

    private Button btnGateway;
    private View mView;

    private Timer timer;
    private TimerTask timerTask;

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
        AylaDevice newDevice = new AylaDevice();
        newDevice.registrationType = AylaNetworks.AML_REGISTRATION_TYPE_BUTTON_PUSH;    //Gateway is always push button
        registerNewDevice(newDevice);
    }

    RegisterHandler _handler;

    private void registerNewDevice(AylaDevice device) {
        MainActivity.getInstance().showWaitDialog(R.string.registering_device_title, R.string.registering_device_body);
        Log.i(LOG_TAG, "rn: Calling registerNewDevice...");

        //This is optional. Add location information to send latitude and longitude during
        // registration of device.
        if(ActivityCompat.checkSelfPermission(getActivity(), "android.permission.ACCESS_COARSE_LOCATION") != PackageManager.PERMISSION_GRANTED){
            requestScanPermissions();
        } else{
            LocationManager locationManager = (LocationManager) getContext().getSystemService(Context
                    .LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_COARSE);
            Location currentLocation;
            List<String> locationProviders = locationManager.getAllProviders();
            for(String provider: locationProviders){
                currentLocation = locationManager.getLastKnownLocation(provider);
                if(currentLocation != null){
                    device.lat = String.valueOf(currentLocation.getLatitude());
                    device.lng = String.valueOf(currentLocation.getLongitude());
                    break;
                }
            }
        }
        _handler = new RegisterHandler(this);
        device.registerNewDevice(_handler);
    }

    static class RegisterHandler extends Handler {
        private WeakReference<ConnectGatewayFragment> _connectGatewayFragment;

        public RegisterHandler(ConnectGatewayFragment connectGatewayFragment) {
            _connectGatewayFragment = new WeakReference<ConnectGatewayFragment>(connectGatewayFragment);
        }

        @Override
        public void handleMessage(Message msg) {
            Logger.logInfo(LOG_TAG, "rn: register handler called: " + msg);
            MainActivity.getInstance().dismissWaitDialog();
            if (msg.arg1 >= 200 && msg.arg1 < 300) {
                // Success!
                // start the device refresh now!
                SessionManager.deviceManager().refreshDeviceList();
                AylaDevice aylaDevice = AylaSystemUtils.gson.fromJson((String) msg.obj, AylaDevice.class);
                Device device = SessionManager.sessionParameters().deviceCreator.deviceForAylaDevice(aylaDevice);
                MainActivity.getInstance().showWaitDialog(R.string.updating_notifications_title, R.string.updating_notifications_body);
                // Now update the device notifications
                DeviceNotificationHelper helper = new DeviceNotificationHelper(device, AylaUser.getCurrent());
                helper.initializeNewDeviceNotifications(new DeviceNotificationHelper.DeviceNotificationHelperListener() {
                    @Override
                    public void newDeviceUpdated(Device device, int error) {
                        Logger.logInfo(LOG_TAG, "rn: newDeviceUpdated [" + device + "]");
                        MainActivity mainActivity = MainActivity.getInstance();
                        mainActivity.dismissWaitDialog();
                        int msgId = (error == AylaNetworks.AML_ERROR_OK ? R.string.registration_success : R.string.registration_success_notification_fail);
                        Toast.makeText(mainActivity, msgId, Toast.LENGTH_LONG).show();
                        if (error == AylaNetworks.AML_ERROR_OK) {
                            // Go to the Gateway display
                            Log.v(LOG_TAG, "rn: registration successful. select gateways.");
                            MainActivity.getInstance().onSelectMenuItemById(R.id.action_gateways);
                        } else {
                            Log.w(LOG_TAG, "rn: registration unsuccessful.");
                        }
                    }
                });

            } else {
                // Something went wrong
                Log.w(LOG_TAG, "rn: could not register device.");
                Toast.makeText(_connectGatewayFragment.get().getActivity(), R.string.registration_failure, Toast.LENGTH_LONG).show();
                exitSetup();
            }
        }
    }

    private static void exitSetup() {
        //if ( _needsExit ) {
            MainActivity.getInstance().showWaitDialog(R.string.exiting_setup_title, R.string.exiting_setup_body);
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    //Looper.prepare();
                    Logger.logVerbose(LOG_TAG, "calling AylaSetup.exit()...");
                    AylaSetup.exit();
                    //Looper.loop();
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    Logger.logVerbose(LOG_TAG, "AylaSetup.exit() completed.");
                   // _needsExit = false;
                    MainActivity.getInstance().dismissWaitDialog();
                }
            }.execute();
        //}
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
    /*
   * Scan needs location permissions. This method requests Location permission
    */
    private void requestScanPermissions(){
        ActivityCompat.requestPermissions(getActivity(), new String[]{"android.permission.ACCESS_COARSE_LOCATION"}, REQUEST_LOCATION);
    }
}
