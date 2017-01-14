package com.aylanetworks.agilelink.beacon;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.ActivityCompat;

import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.fragments.GenericHelpFragment;
import com.aylanetworks.agilelink.framework.automation.Automation;
import com.aylanetworks.agilelink.framework.automation.AutomationManager;
import com.aylanetworks.agilelink.framework.beacon.AMAPBeacon;
import com.aylanetworks.agilelink.framework.beacon.AMAPBeaconManager;
import com.aylanetworks.aylasdk.AylaAPIRequest;

import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;
import com.aylanetworks.aylasdk.error.ServerError;

import org.altbeacon.beacon.BeaconManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fi.iki.elonen.NanoHTTPD;

/*
 * AMAP_Android
 *
 * Copyright 2016 Ayla Networks, all rights reserved
 */

/**
 * BeaconsListFragment displays list of Beacons that were added for this user. The User can edit
 * Beacon name for displayed beacons or delete a selected Beacon by Press and Hold a Beacon item.
 */
public class BeaconsListFragment extends Fragment implements
        ActivityCompat.OnRequestPermissionsResultCallback {
    private final static String LOG_TAG = "BeaconsListFragment";
    private static final int MAX_BEACONS_ALLOWED = 5;

    private ViewHolder _viewHolder;
    private BeaconListAdapter _beaconListAdapter;
    private ListView _listViewBeacons;
    private AlertDialog _alertDialog;
    private static final int REQUEST_COARSE_LOCATION = 2;


    public static BeaconsListFragment newInstance() {
        return new BeaconsListFragment();
    }

    public BeaconsListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_help_automation, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.help_automation:
                showHelpFragment();
                return true;
        }
        return false;
    }

    private void showHelpFragment() {
        String fileURL = MainActivity.getInstance().getString(R.string.automation_help_url);
        MainActivity.getInstance().pushFragment(GenericHelpFragment.newInstance(fileURL));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_all_beacons, container, false);
        _viewHolder = new BeaconsListFragment.ViewHolder();
        _listViewBeacons = (ListView)view.findViewById(R.id.listview_beacons);
        _listViewBeacons.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            public boolean onItemLongClick(AdapterView<?> arg0, View v, int index, long arg3) {
                AMAPBeacon amapBeacon=(AMAPBeacon)_listViewBeacons.getItemAtPosition(index);
                showDeleteWarning(amapBeacon);
                return true;
            }
        });
        _listViewBeacons.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (_listViewBeacons.getAdapter() != null) {
                    AMAPBeacon amapBeacon=(AMAPBeacon)_listViewBeacons.getItemAtPosition(position);
                    if(amapBeacon != null) {
                        updateBeaconName(amapBeacon);
                    }
                }
            }
        });
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getViewHolder().populate(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        isBluetoothEnabled();
         if (checkLocationServices(MainActivity.getInstance())) {
            fetchAndDisplayBeacons();
        }
    }

    private void fetchAndDisplayBeacons() {
        AMAPBeaconManager.fetchBeacons(new Response.Listener<AMAPBeacon[]>() {
            @Override
            public void onResponse(AMAPBeacon[] arrayBeacons) {
                ArrayList<AMAPBeacon> beaconArrayList = new ArrayList<>(Arrays.asList(arrayBeacons));
                Log.i(LOG_TAG, "Fetched " + beaconArrayList.size());
                initAdapter(beaconArrayList);
            }

        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                if (error instanceof ServerError) {
                    //Check if there are no existing beacons. This is not an actual error and we
                    //don't want to show this error.
                    ServerError serverError = ((ServerError) error);
                    int code = serverError.getServerResponseCode();
                    if (code == NanoHTTPD.Response.Status.NOT_FOUND.getRequestStatus()) {
                        initAdapter(new ArrayList<AMAPBeacon>());
                    }
                }
            }
        });

        _viewHolder.actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AddBeaconFragment frag = AddBeaconFragment.newInstance(
                        (ArrayList<AMAPBeacon>) _beaconListAdapter.getBeacons());
                MainActivity.getInstance().pushFragment(frag);
            }
        });

    }

    private void showDeleteWarning(final AMAPBeacon amapBeacon) {
        new AlertDialog.Builder(MainActivity.getInstance())
                .setIcon(R.drawable.ic_launcher)
                .setTitle(R.string.confirm_delete_beacon)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteBeacon(amapBeacon);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create().show();
    }
    private void deleteBeacon(final AMAPBeacon amapBeacon) {
        AMAPBeaconManager.deleteBeacon(amapBeacon, new Response.Listener<AylaAPIRequest.EmptyResponse>() {
            @Override
            public void onResponse(AylaAPIRequest.EmptyResponse response) {
                _beaconListAdapter.remove(amapBeacon);
                deleteBeaconFromAutomation(amapBeacon.getId());
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                String errorString = MainActivity.getInstance().getString(R.string.Toast_Error) +
                        error.toString();
                Toast.makeText(MainActivity.getInstance(), errorString, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * This method Updates Automation(If any) for the deleted Beacon. The user can still reuse
     * this automation to associate with some other location or beacon
     *
     * @param beaconID this is the ID of beacon
     */
    private void deleteBeaconFromAutomation(final String beaconID) {
        if (beaconID == null) {
            return;
        }
        AutomationManager.fetchAutomation(new Response.Listener<Automation[]>() {
            @Override
            public void onResponse(Automation[] response) {
                ArrayList<Automation> automationList= new ArrayList<>(Arrays.asList(response));
                for (Automation automation : automationList) {
                    if (beaconID.equalsIgnoreCase(automation.getTriggerUUID())) {
                        automation.setTriggerUUID("");
                    }
                }
                AutomationManager.updateAutomations(automationList, new Response
                        .Listener<ArrayList<Automation>>() {
                    @Override
                    public void onResponse(ArrayList<Automation> response) {
                        Log.d(LOG_TAG, "Removed Beacon ID " + beaconID);
                        //Make sure we stop Monitoring the region for this Beacon
                        AMAPBeaconService.stopMonitoringRegion(beaconID);
                    }
                }, new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        Log.e(LOG_TAG, error.getMessage());
                    }
                });
            }

        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                Log.e(LOG_TAG, error.getMessage());
            }
        });
    }


    /**
     * This method initializes adapters and also sets the listeners
     *
     * @param beaconList list of Beacons
     */
    private void initAdapter(ArrayList<AMAPBeacon> beaconList) {
        if(beaconList == null) {
            return;
        }
        _beaconListAdapter = new BeaconListAdapter(MainActivity.getInstance(),beaconList);
        _listViewBeacons.setAdapter(_beaconListAdapter);
        int listSize = beaconList.size();
        if (listSize > 0) {
            getViewHolder().emptyState.setVisibility(View.INVISIBLE);
            if (listSize >= MAX_BEACONS_ALLOWED) {
                _viewHolder.actionButton.setVisibility(View.GONE);
            } else {
                _viewHolder.actionButton.setVisibility(View.VISIBLE);
            }

        } else {
            getViewHolder().emptyState.setVisibility(View.VISIBLE);
        }
    }
    private void isBluetoothEnabled() {
        try {
            if (!BeaconManager.getInstanceForApplication(MainActivity.getInstance()).checkAvailability()) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.getInstance());
                builder.setTitle(getString(R.string.bluetooth_title));
                builder.setMessage(getString(R.string.bluetooth_dialog_summary));
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        MainActivity.getInstance().popBackstackToRoot();
                    }
                });
                builder.show();
            }
        }
        catch (RuntimeException e) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.getInstance());
            builder.setTitle(getString(R.string.bluetooth_not_available_title));
            builder.setMessage(getString(R.string.bluetooth_not_available_summary));
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                @Override
                public void onDismiss(DialogInterface dialog) {
                    MainActivity.getInstance().popBackstackToRoot();
                    System.exit(0);
                }

            });
            builder.show();

        }
    }

    private BeaconsListFragment.ViewHolder getViewHolder() {
        return _viewHolder;
    }

    private class ViewHolder {
        ViewGroup emptyState;
        ImageButton actionButton;
        public void populate(View v) {
            emptyState = (ViewGroup) v.findViewById(R.id.fragment_allbeacons_emptyState);
            actionButton = (ImageButton) v.findViewById(R.id.fragment_all_beacons_actionButton);
        }
    }

    private class BeaconListAdapter extends ArrayAdapter<AMAPBeacon> {
        private  class ViewHolder {
            private TextView beaconNameView;
            private TextView beaconIdView;
            private TextView majorVersionView;
            private TextView minorVersionView;
        }
        final ArrayList<AMAPBeacon> _beaconArrayList;

        public BeaconListAdapter(Context c, ArrayList<AMAPBeacon> beaconArrayList) {
            super(c, R.layout.beacon_list, beaconArrayList);
            _beaconArrayList =beaconArrayList;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(R.layout.beacon_list, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.beaconNameView = (TextView) convertView.findViewById(R.id.beacon_name);
                viewHolder.beaconIdView = (TextView) convertView.findViewById(R.id.beacon_id);
                viewHolder.majorVersionView = (TextView) convertView.findViewById(R.id.major_version);
                viewHolder.minorVersionView = (TextView) convertView.findViewById(R.id.minor_version);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            AMAPBeacon amapBeacon = getItem(position);
            if(amapBeacon != null) {
                if (amapBeacon.getName() != null) {
                    viewHolder.beaconNameView.setText(amapBeacon.getName());
                }
                if (amapBeacon.getEddystoneBeaconId() != null) {
                    viewHolder.beaconIdView.setText(amapBeacon.getEddystoneBeaconId());
                }
                if(amapBeacon.getBeaconType().equals(AMAPBeacon.BeaconType.IBeacon)) {
                    viewHolder.beaconIdView.setText(amapBeacon.getProximityUuid());

                    String majorValue = getString(R.string.major_version) +
                            Integer.toString(amapBeacon.getMajorValue());
                    String minorValue = getString(R.string.minor_version) +
                            Integer.toString(amapBeacon.getMinorValue());
                    viewHolder.majorVersionView.setVisibility(View.VISIBLE);
                    viewHolder.minorVersionView.setVisibility(View.VISIBLE);
                    viewHolder.majorVersionView.setText(majorValue);
                    viewHolder.minorVersionView.setText(minorValue);
                } else {
                    viewHolder.majorVersionView.setVisibility(View.GONE);
                    viewHolder.minorVersionView.setVisibility(View.GONE);}
            }
            return convertView;
        }

        public List<AMAPBeacon> getBeacons() {
            return _beaconArrayList;
        }
    }

    private void updateBeaconName(final AMAPBeacon amapBeacon) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.getInstance());
        builder.setTitle(R.string.confirm_update_beacon);

        final EditText input = new EditText(MainActivity.getInstance());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = input.getText().toString();
                        updateBeacon(amapBeacon, name);
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

    private void updateBeacon(AMAPBeacon amapBeacon, String beaconName) {
        if (beaconName ==null || beaconName.trim().length() <=0 ) {
            String msg = MainActivity.getInstance().getString(R.string.invalid_beacon_name);
            Toast.makeText(MainActivity.getInstance(), msg, Toast.LENGTH_LONG).show();
            return;
        }
        amapBeacon.setName(beaconName);
        AMAPBeaconManager.updateBeacon(amapBeacon, new Response.Listener<AylaAPIRequest
                .EmptyResponse>() {
            @Override
            public void onResponse(AylaAPIRequest.EmptyResponse response) {
                String msg = MainActivity.getInstance().getString(R.string.saved_success);
                Toast.makeText(MainActivity.getInstance(), msg, Toast.LENGTH_SHORT).show();
                MainActivity.getInstance().popBackstackToRoot();
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

    private boolean checkLocationServices(final Context context) {
        android.location.LocationManager lm = (android.location.LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
            Log.d(LOG_TAG, ex.getMessage());
        }

        try {
            network_enabled = lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
            Log.d(LOG_TAG, ex.getMessage());
        }

        if (!gps_enabled && !network_enabled) {
            // notify user
            if(_alertDialog == null) {
                _alertDialog = getAlertDialog(context);
            }
            if(!_alertDialog.isShowing()) {
                _alertDialog.show();
            }
        } else {
            if (ActivityCompat.checkSelfPermission(MainActivity.getInstance(), Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.getInstance(),
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        REQUEST_COARSE_LOCATION);

                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.getInstance().popBackstackToRoot();
                    }
                };
                Handler h = new Handler();
                int POST_DELAYED_TIME_MS = 30;
                h.postDelayed(r, POST_DELAYED_TIME_MS);
            }
            else {
                return true;
            }
        }
        return false;
    }

    private AlertDialog getAlertDialog(final Context context) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setMessage(context.getResources().getString(R.string.gps_network_not_enabled));
        dialog.setPositiveButton(context.getResources().getString(R.string.open_location_settings), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                context.startActivity(myIntent);
            }
        });
        dialog.setNegativeButton(context.getString(android.R.string.cancel), new DialogInterface
                .OnClickListener() {
            @Override
            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                Toast.makeText(context, context.getString(R.string.location_permission_required_toast), Toast.LENGTH_SHORT).show();
            }
        });
        return dialog.create();
    }
}
