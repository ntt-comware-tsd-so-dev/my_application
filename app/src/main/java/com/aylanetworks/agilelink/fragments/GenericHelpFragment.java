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
public class GenericHelpFragment extends Fragment {
    private final static String ARG_FILE_URL = "file_url";

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.automation_help, container, false);
        if (getArguments() != null) {
            String fileURL = getArguments().getString(ARG_FILE_URL);
            WebView webView = (WebView) root.findViewById(R.id.webview);
            webView.loadUrl(fileURL);
        }
        return root;
    }
    public static GenericHelpFragment newInstance(String fileURL) {
        GenericHelpFragment fragment = new GenericHelpFragment();
        if(fileURL != null) {
            Bundle args = new Bundle();
            args.putString(ARG_FILE_URL, fileURL);
            fragment.setArguments(args);
        }
        return fragment;
    }
}