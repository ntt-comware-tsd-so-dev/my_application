package com.aylanetworks.agilelink.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.aylanetworks.agilelink.R;

/**
 * Created by Brian King on 1/28/15.
 */
public class SettingsFragment extends Fragment implements AdapterView.OnItemClickListener {
    private final static String LOG_TAG = "SettingsFragment";
    private ListView _listView;

    // List view indexes
    private final int INDEX_REGISTRATION = 0;
    private final int INDEX_WIFI_SETUP = 1;
    private final int INDEX_PROFILE = 2;
    private final int INDEX_EMAIL_LOGS = 3;

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        _listView = (ListView)view.findViewById(R.id.listView);
        ArrayAdapter<CharSequence>adapter = ArrayAdapter.createFromResource(getActivity(), R.array.settings_items, android.R.layout.simple_list_item_1);
        _listView.setAdapter(adapter);
        _listView.setOnItemClickListener(this);

        return view;
    }

    private void handleRegistration() {
        // Bring up the Add Device UI
        AddDeviceFragment frag = AddDeviceFragment.newInstance();
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.anim.abc_fade_in, R.anim.abc_fade_out,
                R.anim.abc_fade_in, R.anim.abc_fade_out);
        ft.add(android.R.id.content, frag).addToBackStack(null).commit();
    }

    private void handleWiFiSetup() {
        WiFiSetupFragment frag = WiFiSetupFragment.newInstance();
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.anim.abc_fade_in, R.anim.abc_fade_out,
                R.anim.abc_fade_in, R.anim.abc_fade_out);
        ft.add(android.R.id.content, frag).addToBackStack(null).commit();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d(LOG_TAG, "onItemClick: " + position);
        switch ( position ) {
            case INDEX_REGISTRATION:
                handleRegistration();
                break;

            case INDEX_WIFI_SETUP:
                handleWiFiSetup();
                break;

            case INDEX_PROFILE:
            case INDEX_EMAIL_LOGS:
            default:
                Toast.makeText(getActivity(), "Coming soon!", Toast.LENGTH_SHORT).show();
        }
    }
}
