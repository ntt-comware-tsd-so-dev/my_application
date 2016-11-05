package com.aylanetworks.agilelink.geofence;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import com.android.volley.Response;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.automation.Automation;
import com.aylanetworks.agilelink.framework.automation.AutomationManager;
import com.aylanetworks.agilelink.framework.geofence.GeofenceLocation;
import com.aylanetworks.agilelink.framework.geofence.LocationManager;
import com.aylanetworks.aylasdk.AylaAPIRequest;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;
import com.aylanetworks.aylasdk.error.ServerError;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fi.iki.elonen.NanoHTTPD;

/*
 * AMAP_Android
 *
 * Copyright 2016 Ayla Networks, all rights reserved
 */

public class AllGeofencesFragment extends Fragment {
    private final static String LOG_TAG = "AllGeofencesFragment";
    private ViewHolder _viewHolder;
    private AddGeofenceFragment _dialogFragment;
    private AllGeofencesAdapter _allGeofencesAdapter;
    private static final int MAX_GEOFENCES_ALLOWED = 5;

    public static AllGeofencesFragment newInstance() {
        return new AllGeofencesFragment();
    }

    public AllGeofencesFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        GeofenceController.getInstance().init(MainActivity.getInstance());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_all_geofences, container, false);
        _viewHolder = new ViewHolder();
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getViewHolder().populate(view);
        _viewHolder.geofenceRecyclerView.setHasFixedSize(true);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        _viewHolder.geofenceRecyclerView.setLayoutManager(layoutManager);
        LocationManager.fetchGeofenceLocations(new Response.Listener<GeofenceLocation[]>() {
            @Override
            public void onResponse(GeofenceLocation[] arrayGeofences) {
                //arrayGeofences are all the Geofence locations stored in the Datum field
                List<GeofenceLocation> geofenceLocations = new ArrayList<>(Arrays.asList(arrayGeofences));
                //Now get the list of Geofences that are not added from this phone.
                SharedPreferences prefs = MainActivity.getInstance().getSharedPreferences(GeofenceController.SHARED_PERFS_GEOFENCE,
                        Context.MODE_PRIVATE);
                List <GeofenceLocation> geofencesNotAdded = LocationManager.getGeofencesNotInPrefs
                        (prefs,geofenceLocations);
                if(geofencesNotAdded != null && !geofencesNotAdded.isEmpty()){
                        addMissingGeofences(geofencesNotAdded, geofenceLocations);
                }
                else {
                    initAdapter(geofenceLocations);
                }
            }

        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                if (error instanceof ServerError) {
                    //Check if there are no existing automations. This is not an actual error and we
                    //don't want to show this error.
                    ServerError serverError = ((ServerError) error);
                    int code = serverError.getServerResponseCode();
                    if(code == NanoHTTPD.Response.Status.NOT_FOUND.getRequestStatus()) {
                        initAdapter(new ArrayList<GeofenceLocation>());
                    }
                }
            }
        });

        _viewHolder.actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isLocationEnabled(MainActivity.getInstance())) {
                    return;
                }
                _dialogFragment = new AddGeofenceFragment();
                _dialogFragment.setListener(AllGeofencesFragment.this);
                _dialogFragment.show(getActivity().getSupportFragmentManager(), "AddGeofenceFragment");
            }
        });
    }

    /**
     * This method initializes adapters and also sets the listeners
     * @param geofenceLocations list of geofenceLocations
     */
    private void initAdapter(List<GeofenceLocation> geofenceLocations) {
        GeofenceController.getInstance().setALGeofenceLocations(geofenceLocations);

        _allGeofencesAdapter = new AllGeofencesAdapter(GeofenceController.getInstance().getALGeofenceLocations());
        _viewHolder.geofenceRecyclerView.setAdapter(_allGeofencesAdapter);
        refresh();
        _allGeofencesAdapter.setListener(new AllGeofencesAdapter.AllGeofencesAdapterListener() {
            @Override
            public void onDeleteTapped(final GeofenceLocation alGeofenceLocation) {

                LocationManager.deleteGeofenceLocation(alGeofenceLocation, new Response.Listener<AylaAPIRequest.EmptyResponse>() {
                    @Override
                    public void onResponse(AylaAPIRequest.EmptyResponse response) {
                        List<GeofenceLocation> GeofenceLocations = new ArrayList<>();
                        GeofenceLocations.add(alGeofenceLocation);
                        GeofenceController.getInstance().removeGeofences(GeofenceLocations, geofenceControllerListener);
                        deleteAutomationLocation(alGeofenceLocation.getId());
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
        });
    }

    /**
     * This method deletes Automation(If any) for the deleted Location. Without this Location the
     * Automation is invalid
     * @param locationUUID this is the uuid of the GeofenceLocation that was deleted
     */
    private void deleteAutomationLocation(final String locationUUID) {
       if(locationUUID == null) {
           return;
       }
        AutomationManager.fetchAutomation(new Response.Listener<Automation[]>() {
            @Override
            public void onResponse(Automation[] response) {
                for (Automation automation : response) {
                    if (locationUUID.equalsIgnoreCase(automation.getTriggerUUID())) {
                        AutomationManager.deleteAutomation(automation, new Response.Listener<AylaAPIRequest
                                .EmptyResponse>() {
                            @Override
                            public void onResponse(AylaAPIRequest.EmptyResponse response) {
                                Log.d(LOG_TAG, "Deleted Automation for Location UUID " +locationUUID);
                            }
                        }, new ErrorListener() {
                            @Override
                            public void onErrorResponse(AylaError error) {
                                Log.e(LOG_TAG, error.getMessage());
                            }
                        });
                    }
                }
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                Log.e(LOG_TAG, error.getMessage());
            }
        });
    }

    /**
     * This method will add all the geofence locations that are added in some other Phone with
     * the same User credentials
     * @param toAddGeofenceList This is the list that need to be added
     * @param allGeofenceLocations These locations are already added on this Phone
     */
    private void addMissingGeofences(List<GeofenceLocation> toAddGeofenceList,
                                    List<GeofenceLocation> allGeofenceLocations) {
        MainActivity.getInstance().showWaitDialog(R.string.add_geofence, R.string.geofences_added_other_phone);
        for(GeofenceLocation geofence:toAddGeofenceList) {
            GeofenceController.getInstance().addGeofence(geofence, geofenceControllerListener);
        }
        //Noe remove the ones we added above
        if(allGeofenceLocations.removeAll(toAddGeofenceList)) {
            initAdapter(allGeofenceLocations);
        }
        MainActivity.getInstance().dismissWaitDialog();
    }

    public void addGeofence(final GeofenceLocation geofence) {
        LocationManager.addGeofenceLocation(geofence, new Response.Listener<AylaAPIRequest.EmptyResponse>() {
            @Override
            public void onResponse(AylaAPIRequest.EmptyResponse response) {
                GeofenceController.getInstance().addGeofence(geofence, geofenceControllerListener);
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

    private final GeofenceController.GeofenceControllerListener geofenceControllerListener = new
            GeofenceController.GeofenceControllerListener() {
                @Override
                public void onGeofencesUpdated() {
                    refresh();
                }

                @Override
                public void onError() {
                    showErrorToast();
                }
            };

    private void refresh() {
        _allGeofencesAdapter.notifyDataSetChanged();
        int countGeofenceLocations = _allGeofencesAdapter.getItemCount();

        if (countGeofenceLocations > 0) {
            getViewHolder().emptyState.setVisibility(View.INVISIBLE);
            if(countGeofenceLocations >= MAX_GEOFENCES_ALLOWED) {
                _viewHolder.actionButton.setVisibility(View.GONE);
            } else{
                _viewHolder.actionButton.setVisibility(View.VISIBLE);
            }

        } else {
            getViewHolder().emptyState.setVisibility(View.VISIBLE);

        }

        getActivity().invalidateOptionsMenu();
    }

    private void showErrorToast() {
        Toast.makeText(getActivity(), getActivity().getString(R.string.Toast_Error), Toast.LENGTH_SHORT).show();
    }

    public static boolean isLocationEnabled(final Context context) {
        android.location.LocationManager lm = (android.location.LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {
            Log.d(LOG_TAG, ex.getMessage());
        }

        try {
            network_enabled = lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER);
        } catch(Exception ex) {
            Log.d(LOG_TAG, ex.getMessage());
        }

        if(!gps_enabled && !network_enabled) {
            // notify user
            AlertDialog.Builder dialog = new AlertDialog.Builder(context);
            dialog.setMessage(context.getResources().getString(R.string.gps_network_not_enabled));
            dialog.setPositiveButton(context.getResources().getString(R.string.open_location_settings), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    Intent myIntent = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS);
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
            dialog.show();
        }
        else{
            return true;
        }
        return false;
    }

    @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);
        if (reqCode == MainActivity.PLACE_PICKER_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                Place place = PlacePicker.getPlace(getActivity(), data);
                StringBuilder stBuilder = new StringBuilder();
                String address = String.format("%s", place.getAddress());
                stBuilder.append("\n");
                stBuilder.append("Address: ");
                stBuilder.append(address);
                if (_dialogFragment != null) {
                    _dialogFragment.setAddress(stBuilder.toString());
                }
            }
        }
    }
    
    private ViewHolder getViewHolder() {
        return _viewHolder;
    }
    
    class ViewHolder {
        ViewGroup container;
        ViewGroup emptyState;
        ImageButton actionButton;
        RecyclerView geofenceRecyclerView;

        public void populate(View v) {
            container = (ViewGroup) v.findViewById(R.id.fragment_all_geofences_container);
            emptyState = (ViewGroup) v.findViewById(R.id.fragment_all_geofences_emptyState);
            geofenceRecyclerView = (RecyclerView) v.findViewById(R.id.recycler_view);
            actionButton = (ImageButton) v.findViewById(R.id.fragment_all_geofences_actionButton);
        }
    }
}
