package com.aylanetworks.agilelink.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.aylanetworks.aaml.AylaUser;
import com.aylanetworks.agilelink.R;

import com.aylanetworks.agilelink.fragments.adapters.DeviceListAdapter;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.DeviceManager;
import com.aylanetworks.agilelink.framework.SessionManager;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p/>
 */
public class AllDevicesFragment extends Fragment
        implements AbsListView.OnItemClickListener, Device.DeviceStatusListener,
        DeviceManager.DeviceListListener, SessionManager.SessionListener {

    private final static String LOG_TAG="AllDevicesFragment";

    public final static int DISPLAY_MODE_ALL = 0;
    public final static int DISPLAY_MODE_FAVORITES = 1;

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_DISPLAY_MODE = "display_mode";

    /**
     * Mode we should display
     */
    private int _displayMode = DISPLAY_MODE_ALL;

    /**
     * The fragment's ListView/GridView.
     */
    private AbsListView _listView;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private ListAdapter _adapter;

    public static AllDevicesFragment newInstance(int displayMode) {
        AllDevicesFragment fragment = new AllDevicesFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_DISPLAY_MODE, displayMode);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public AllDevicesFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            _displayMode = getArguments().getInt(ARG_DISPLAY_MODE);
        }

        // See if we have a device manager yet
        DeviceManager dm = SessionManager.deviceManager();
        if ( dm != null ) {
            _adapter = new ArrayAdapter<Device>(getActivity(),
                    android.R.layout.simple_list_item_1, android.R.id.text1, dm.deviceList());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_aldevice, container, false);

        // Set up the list view

        _listView = (AbsListView) view.findViewById(android.R.id.list);
        _listView.setAdapter(_adapter);

        // Set OnItemClickListener so we can be notified on item clicks
        _listView.setOnItemClickListener(this);
        
        setEmptyText(getString(R.string.no_devices_found));

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        SessionManager.addSessionListener(this);

        DeviceManager deviceManager = SessionManager.deviceManager();
        if ( deviceManager != null ) {
            SessionManager.deviceManager().addDeviceListListener(this);
            SessionManager.deviceManager().addDeviceStatusListener(this);
        }

        /*
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
        */
    }

    @Override
    public void onDetach() {
        super.onDetach();

        SessionManager.removeSessionListener(this);

        DeviceManager deviceManager = SessionManager.deviceManager();
        if ( deviceManager != null ) {
            SessionManager.deviceManager().removeDeviceListListener(this);
            SessionManager.deviceManager().removeDeviceStatusListener(this);
        }
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Device d = (Device)_listView.getItemAtPosition(position);
        if ( d != null ) {
            DeviceDetailFragment frag = DeviceDetailFragment.newInstance(d);
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.setCustomAnimations(R.anim.abc_fade_in, R.anim.abc_fade_out,
                                   R.anim.abc_fade_in, R.anim.abc_fade_out);
            ft.add(android.R.id.content, frag).addToBackStack(null).commit();
        }
    }

    /**
     * The default content for this Fragment has a TextView that is shown when
     * the list is empty. If you would like to change the text, call this method
     * to supply the text it should use.
     */
    public void setEmptyText(CharSequence emptyText) {
        View emptyView = _listView.getEmptyView();

        if (emptyView instanceof TextView) {
            ((TextView) emptyView).setText(emptyText);
        }
    }

    @Override
    public void deviceListChanged() {
        Log.i(LOG_TAG, "Device list changed");
        _adapter = new DeviceListAdapter(getActivity(), SessionManager.deviceManager().deviceList());
        _listView.setAdapter(_adapter);
    }

    @Override
    public void statusUpdated(Device device) {
        Log.i(LOG_TAG, "Device " + device.getDevice().productName + " changed");
        _listView.setAdapter(_adapter);
    }

    @Override
    public void loginStateChanged(boolean loggedIn, AylaUser aylaUser) {
        DeviceManager deviceManager = SessionManager.deviceManager();
        if (deviceManager != null) {
            if (loggedIn) {
                SessionManager.deviceManager().addDeviceListListener(this);
                SessionManager.deviceManager().addDeviceStatusListener(this);
            } else {
                // Logged out
                SessionManager.deviceManager().removeDeviceListListener(this);
                SessionManager.deviceManager().removeDeviceStatusListener(this);
            }
        }
    }

    @Override
    public void reachabilityChanged(int reachabilityState) {
        Log.v(LOG_TAG, "Reachability changed: " + reachabilityState);
    }

    @Override
    public void lanModeChanged(boolean lanModeEnabled) {
        Log.v(LOG_TAG, "lanModeChanged: " + (lanModeEnabled ? "ENABLED" : "DISABLED"));
    }
}
