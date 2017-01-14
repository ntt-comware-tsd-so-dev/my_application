
package com.aylanetworks.agilelink.beacon;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.Response;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.automation.Automation;
import com.aylanetworks.agilelink.framework.beacon.AMAPBeacon;
import com.aylanetworks.agilelink.framework.beacon.AMAPBeaconManager;
import com.aylanetworks.aylasdk.AylaAPIRequest;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.support.v4.app.Fragment;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

/*
 * AMAP_Android
 *
 * Copyright 2016 Ayla Networks, all rights reserved
 */

/**
 * AddBeaconFragment class is used to scan and add new Beacons.
 */
public class AddBeaconFragment extends Fragment implements BeaconConsumer {
    private static final String TAG = "AddBeaconFragment";
    private final static String BEACON_LIST = "beacon_list";


    private BeaconManager _beaconManager;
    private ListView _listView;
    private ArrayList<AMAPBeacon> _beaconArrayList;
    private boolean _foundBeacons = false;
    private Map<String,Beacon> _beaconMap;
    private final static int EDDYSTONE_SERVICEUUID= 0xfeaa;

    public AddBeaconFragment() {
        // Required empty public constructor
    }

    public static AddBeaconFragment newInstance(ArrayList<AMAPBeacon> beaconArrayList) {
        AddBeaconFragment fragment = new AddBeaconFragment();
        if (beaconArrayList != null) {
            Bundle args = new Bundle();
            args.putSerializable(BEACON_LIST, beaconArrayList);
            fragment.setArguments(args);
        }
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _beaconManager = BeaconManager.getInstanceForApplication(getActivity());
        _beaconManager.getBeaconParsers().clear();
        List<BeaconParser> parserList = _beaconManager.getBeaconParsers();
        parserList.add(new BeaconParser().setBeaconLayout(AMAPBeaconService
                .EDDYSTONE_BEACON_LAYOUT));
        parserList.add(new BeaconParser().setBeaconLayout(AMAPBeaconService
                .IBEACON_LAYOUT));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.add_beacon, container, false);
        _beaconManager.bind(this);
        if (getArguments() != null) {
            _beaconArrayList = (ArrayList<AMAPBeacon>) getArguments().getSerializable(BEACON_LIST);
        }
        _listView = (ListView) view.findViewById(R.id.beacon_list);
        _listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (_listView.getAdapter() != null) {
                    final String beaconId = (String) _listView.getAdapter().getItem(position);
                    inputBeaconName(beaconId);
                }
            }
        });
        return view;
    }

    private void inputBeaconName(final String beaconId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.getInstance());
        builder.setTitle(R.string.confirm_add_beacon);

        final EditText input = new EditText(MainActivity.getInstance());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = input.getText().toString();
                        addBeacon(beaconId, name);
                    }
                }
        );
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }
        );
        builder.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (_beaconManager.isBound(this)) {
            _beaconManager.setBackgroundMode(false);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (_beaconManager.isBound(this)) {
            _beaconManager.setBackgroundMode(true);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        _beaconManager.unbind(this);
    }

    @Override
    public void onBeaconServiceConnect() {
        try {
            final long SCAN_DURATION = 2000; //2 seconds
            final long SCAN_BETWEEN_DURATION = 0;//

            //Scan lasts for SCAN_PERIOD time
            _beaconManager.setForegroundScanPeriod(SCAN_DURATION);

            //Wait every SCAN_PERIOD_IN_BETWEEN time
            _beaconManager.setForegroundBetweenScanPeriod(SCAN_BETWEEN_DURATION);

            //Update default time with the new one
            _beaconManager.updateScanPeriods();
        } catch (RemoteException e) {
            Log.e(TAG, "onBeaconServiceConnect" + e.getMessage());
            return;
        }
        doBeaconRanging();
    }

    private void doBeaconRanging() {
        MainActivity.getInstance().showWaitDialog(R.string.fetching_beacons_title, R.string.fetching_beacons_body);

        final Region region = new Region("myRangingUniqueId", null, null, null);
        _beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    //Don't display the Beacons we already added
                    if (_beaconArrayList != null && _beaconArrayList.size() > 0) {
                        Iterator<Beacon> beaconIterator = beacons.iterator();
                        while (beaconIterator.hasNext()) {
                            Beacon beacon = beaconIterator.next();
                            for (AMAPBeacon amapBeacon : _beaconArrayList) {
                                //Check if it is Eddystone Beacon
                                if(beacon.getServiceUuid() == EDDYSTONE_SERVICEUUID) {
                                    if (beacon.toString().equals(amapBeacon.getEddystoneBeaconId())) {
                                        beaconIterator.remove();
                                    }
                                } else {
                                    // This might be iBeacon. Now compare id along with major
                                    // version and minor version
                                    Identifier id1 = beacon.getId1();
                                    Identifier id2 = beacon.getId2();
                                    Identifier id3 = beacon.getId3();
                                    if(id2 == null || id3 == null) {
                                        continue;
                                    }
                                    if (id1.toString().equals(amapBeacon.getProximityUuid()) &&
                                            id2.toString().equals(Integer.toString(amapBeacon.getMajorValue())) &&
                                    id3.toString().equals(Integer.toString(amapBeacon.getMinorValue()))){
                                        beaconIterator.remove();
                                    }
                                }
                            }
                        }
                    }

                    if (beacons.size() > 0) {
                        final ArrayList<Beacon> beaconsList = new ArrayList<>(beacons);
                        _foundBeacons = true;
                        if(_beaconMap == null) {
                            _beaconMap = new HashMap<>();
                        } else {
                            _beaconMap.clear();
                        }
                        String beaconNames[] = new String[beaconsList.size()];
                        for (int idx = 0; idx < beaconsList.size(); idx++) {
                            Beacon beacon = beaconsList.get(idx);
                            beaconNames[idx] = beacon.toString();
                            _beaconMap.put(beaconNames[idx],beacon);
                        }
                        try {
                            _beaconManager.stopRangingBeaconsInRegion(region);
                        } catch (RemoteException e) {
                            Log.e(TAG, "stopRangingBeaconsInRegion" + e.getMessage());
                            MainActivity.getInstance().dismissWaitDialog();
                        }

                        displayBeacons(beaconNames);
                        MainActivity.getInstance().dismissWaitDialog();
                    }
                }
            }
        });
        try {
            //if user does not find any Beacons in 10 seconds display alert dialog
            int timeoutInSeconds = 10;
            _beaconManager.startRangingBeaconsInRegion(region);
            final Handler timeoutHandler = new Handler(Looper.getMainLooper());
            timeoutHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //check if we found any new beacons with in time span
                    if (!_foundBeacons) {
                        try {
                            _beaconManager.stopRangingBeaconsInRegion(region);
                        } catch (RemoteException e) {
                            Log.e(TAG, "stopRangingBeaconsInRegion" + e.getMessage());
                        }
                        MainActivity.getInstance().dismissWaitDialog();
                        showScanAlertDialog();
                    }
                }
            }, timeoutInSeconds * 1000);
        } catch (RemoteException e) {
            Log.e(TAG, "startRangingBeaconsInRegion" + e.getMessage());
            MainActivity.getInstance().dismissWaitDialog();
        }

    }

    private void showScanAlertDialog() {
        new AlertDialog.Builder(MainActivity.getInstance())
                .setMessage(R.string.no_beacons_found)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        doBeaconRanging();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity.getInstance().onBackPressed();
                    }
                })
                .create().show();

    }

    private void displayBeacons(final String[] beaconNames) {
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.getInstance(), android.R.layout
                .simple_list_item_activated_1, beaconNames);
        MainActivity.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _listView.setAdapter(adapter);
            }
        });
    }

    private void addBeacon(String beaconId, String beaconName) {
        if (beaconName ==null || beaconName.trim().length() <=0 ) {
            String msg = MainActivity.getInstance().getString(R.string.invalid_beacon_name);
            Toast.makeText(MainActivity.getInstance(), msg, Toast.LENGTH_LONG).show();
            return;
        }
        AMAPBeacon amapBeacon = new AMAPBeacon();
        amapBeacon.setId(Automation.randomUUID());
        amapBeacon.setName(beaconName);
        //check if the beacon is EddyStone or iBeacon
        Beacon beacon = _beaconMap.get(beaconId);
        if(beacon.getServiceUuid() == EDDYSTONE_SERVICEUUID) {
            // This is Eddystone, which uses a service Uuid of 0xfeaa
            amapBeacon.setBeaconType(AMAPBeacon.BeaconType.EddyStone);
            amapBeacon.setEddystoneBeaconId(beaconId);
        } else {
            // This might be iBeacon that has Major and Minor Values
            String uuid = beacon.getId1().toString();
            String major = beacon.getId2().toString();
            String minor = beacon.getId3().toString();
            if(uuid ==null || major == null|| minor == null) {
                Log.e(TAG, "Unknown Beacon type for Id " + beaconId);
                return;
            }
            int majorValue =0;
            int minorValue =0;
            try {
                majorValue=Integer.parseInt(major);
                minorValue= Integer.parseInt(minor);
            } catch(NumberFormatException e) {
                Log.e(TAG, "Unknown Beacon type for Id " + beaconId);
                return;
            }
            amapBeacon.setBeaconType(AMAPBeacon.BeaconType.IBeacon);
            amapBeacon.setProximityUuid(uuid);
            amapBeacon.setMajorValue(majorValue);
            amapBeacon.setMinorValue(minorValue);
        }

        AMAPBeaconManager.addBeacon(amapBeacon, new Response.Listener<AylaAPIRequest
                .EmptyResponse>() {
            @Override
            public void onResponse(AylaAPIRequest.EmptyResponse response) {
                String msg = MainActivity.getInstance().getString(R.string.saved_success);
                Toast.makeText(MainActivity.getInstance(), msg, Toast.LENGTH_SHORT).show();
                MainActivity.getInstance().onBackPressed();
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                String errorString = MainActivity.getInstance().getString(R.string.Toast_Error) +
                        error.toString();
                Toast.makeText(MainActivity.getInstance(), errorString, Toast.LENGTH_LONG).show();
                MainActivity.getInstance().popBackstackToRoot();
            }
        });
    }

    @Override
    public Context getApplicationContext() {
        return getActivity().getApplicationContext();
    }

    @Override
    public void unbindService(ServiceConnection serviceConnection) {
        getActivity().unbindService(serviceConnection);
    }

    @Override
    public boolean bindService(Intent intent, ServiceConnection serviceConnection, int mode) {
        return getActivity().bindService(intent, serviceConnection, mode);
    }
}
