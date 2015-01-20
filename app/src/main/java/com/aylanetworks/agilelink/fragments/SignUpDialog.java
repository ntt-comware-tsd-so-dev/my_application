package com.aylanetworks.agilelink.fragments;

import android.app.ActionBar;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import com.aylanetworks.agilelink.R;

/**
 * Created by Brian King on 1/20/15.
 */
public class SignUpDialog extends Dialog {
    public SignUpDialog(final Context context) {
        super(context, R.style.FullHeightDialog);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.sign_up);

        // Make the dialog full-screen
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.MATCH_PARENT;
        getWindow().setAttributes(params);

        ScrollView sv = (ScrollView)findViewById(R.id.ScrollView01);
        sv.requestLayout();

        RelativeLayout rl = (RelativeLayout)findViewById(R.id.signInRelativeLayout);
        rl.requestLayout();
    }
}
