package com.aylanetworks.agilelink.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import com.aylanetworks.agilelink.R;
/*
 * AMAP_Android
 *
 * Copyright 2016 Ayla Networks, all rights reserved
 */
public class AutomationHelpFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.automation_help, container, false);
        WebView webView = (WebView) root.findViewById(R.id.webview);
        webView.loadUrl("file:///android_res/raw/automation_help.htm");
        return root;
    }
    public static AutomationHelpFragment newInstance() {
        return new AutomationHelpFragment();
    }
}