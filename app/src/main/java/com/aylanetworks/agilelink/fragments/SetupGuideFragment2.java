package com.aylanetworks.agilelink.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;

import java.util.Timer;
import java.util.TimerTask;


/**
 * Created by Emmanuel Luna on 25/06/2015.
 */
public class SetupGuideFragment2 extends Fragment implements View.OnClickListener {

    private Button mReady;
    private View mView;

    private Timer timer;
    private TimerTask timerTask;

    private final Handler uiHandler = new Handler();

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.setup_guide_page_2, container, false);
        mReady = (Button)mView.findViewById(R.id.btn_ready);
        mReady.setOnClickListener(this);
        return mView;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_ready:
                MainActivity.getInstance().pushFragment(new ConnectGatewayFragment());
                break;
        }
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
        if(frame%12 == 0){
            if(activity.findViewById(R.id.redled1) != null){
                activity.findViewById(R.id.redled1).setVisibility(activity.findViewById(R.id.redled1).getVisibility() == View.VISIBLE?View.GONE:View.VISIBLE);
            }
        }

        if(frame%24 == 0 || frame%30 == 0 || frame%33 == 0 || frame%36 == 0){
            if(activity.findViewById(R.id.greenled1) != null){
                activity.findViewById(R.id.greenled1).setVisibility(activity.findViewById(R.id.greenled1).getVisibility() == View.VISIBLE?View.GONE:View.VISIBLE);
            }
        }
    }

}


