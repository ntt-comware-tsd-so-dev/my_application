package com.aylanetworks.agilelink.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;



/**
 * Created by Emmanuel Luna on 25/06/2015.
 */
public class SetupGuideFragment1 extends Fragment implements View.OnClickListener {

    private Button mConnected;
    private View mView;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.setup_guide_page_1, container, false);
        mConnected = (Button)mView.findViewById(R.id.btn_connected);
        mConnected.setOnClickListener(this);
        return mView;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_connected:
                MainActivity.getInstance().pushFragment(new SetupGuideFragment2());
                break;
        }
    }
}


