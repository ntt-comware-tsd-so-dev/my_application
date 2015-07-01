package com.aylanetworks.agilelink.fragments;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.aylanetworks.agilelink.R;


/**
 * Created by Emmanuel Luna on 06/18/15.
 */
public class WelcomeFragment extends Fragment implements View.OnClickListener {

    private ImageView mGatewayImage;
    private Button mGetStarted;

    private View mView;

    public static WelcomeFragment newInstance() {
        return new WelcomeFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_welcome, container, false);

        mGatewayImage = (ImageView)mView.findViewById(R.id.gateway_img);
        mGatewayImage.setOnClickListener(this);

        mGetStarted = (Button)mView.findViewById(R.id.setup_btn);
        mGetStarted.setOnClickListener(this);

        return mView;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.gateway_img:
            case R.id.setup_btn:
             {
                SetupGuideFragment1 frag = new SetupGuideFragment1();
                FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
                ft.setCustomAnimations(R.anim.abc_fade_in, R.anim.abc_fade_out,
                        R.anim.abc_fade_in, R.anim.abc_fade_out);
                // For the pager navigation, we push the fragment
                ft.add(android.R.id.content, frag).addToBackStack(null).commit();
                break;
            }
        }
    }

}
