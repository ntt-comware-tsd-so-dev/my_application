package com.aylanetworks.agilelink.fragments;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.aylanetworks.aaml.AylaLogManager;
import com.aylanetworks.aaml.AylaSystemUtils;
import com.aylanetworks.agilelink.BuildConfig;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.SessionManager;

/**
 * Created by Emmanuel Luna on 06/16/15.
 */
public class HelpFragment extends Fragment implements View.OnClickListener {

    private View mAboutAMAPView;
    private View mGetStarted;
    private View mGuidesView;
    private View mTermsAndConditionView;
    private View mEmailLogsView;
    private View mView;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_help, container, false);

        mAboutAMAPView = mView.findViewById(R.id.about_amap);
        mAboutAMAPView.setOnClickListener(this);

        mGetStarted = mView.findViewById(R.id.get_started);
        mGetStarted.setOnClickListener(this);

        mGuidesView = mView.findViewById(R.id.guides_videos);
        mGuidesView.setOnClickListener(this);

        mTermsAndConditionView = mView.findViewById(R.id.terms_and_condition);
        mTermsAndConditionView.setOnClickListener(this);

        mEmailLogsView = mView.findViewById(R.id.email_logs);
        mEmailLogsView.setOnClickListener(this);

        ((TextView)mView.findViewById(R.id.version)).setText(BuildConfig.VERSION_NAME);

        return mView;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.about_amap:
                Intent aboutIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.about_amap_link)));
                startActivity(aboutIntent);
                break;

            case R.id.get_started:
                Intent faqIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.get_started_link)));
                startActivity(faqIntent);
                break;

            case R.id.guides_videos:
                Intent userGuideIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.guides_videos_link)));
                startActivity(userGuideIntent);
                break;

            case R.id.terms_and_condition:
                Intent termsAndConditionIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(	getResources().getString(R.string.terms_and_condition_link)));
                startActivity(termsAndConditionIntent);
                break;

            case R.id.email_logs:
                Intent emailLogsIntent = AylaSystemUtils.emailLogsToSupport(SessionManager.sessionParameters().appId);
                if (emailLogsIntent != null) {
                    startActivity(emailLogsIntent);
                }
                break;
        }
    }

}