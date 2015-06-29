package com.aylanetworks.agilelink.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

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
                {
                    SetupGuideFragment2 frag = new SetupGuideFragment2();
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


