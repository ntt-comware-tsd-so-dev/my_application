package com.aylanetworks.agilelink.geofence;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import com.aylanetworks.agilelink.framework.geofence.GeofenceLocation;
import com.aylanetworks.agilelink.framework.geofence.LocationManager;
import com.aylanetworks.aylasdk.AylaAPIRequest;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
                List<GeofenceLocation> geofenceLocations = new ArrayList<>();
                geofenceLocations.addAll(Arrays.asList(arrayGeofences));
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
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                Log.d(LOG_TAG, error.getMessage());
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

    private ArrayList<GeofenceLocation> loadGeofences() {
        final ArrayList<GeofenceLocation> locationArrayList = new ArrayList<>();
        LocationManager.fetchGeofenceLocations(new Response.Listener<GeofenceLocation[]>() {
            @Override
            public void onResponse(GeofenceLocation[] response) {
                locationArrayList.addAll(Arrays.asList(response));
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                Toast.makeText(MainActivity.getInstance(), "Failed to load geofences:" + error
                        .toString(), Toast.LENGTH_LONG).show();
            }
        });
        return locationArrayList;
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

        if (_allGeofencesAdapter.getItemCount() > 0) {
            getViewHolder().emptyState.setVisibility(View.INVISIBLE);
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
