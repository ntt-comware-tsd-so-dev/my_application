package com.aylanetworks.agilelink.geofence;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
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
import com.aylanetworks.agilelink.framework.geofence.ALGeofenceLocation;
import com.aylanetworks.agilelink.framework.geofence.ALLocationManager;
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
        Runnable r = new Runnable() {
            @Override
            public void run() {
                ALLocationManager.fetchGeofenceLocations(new Response.Listener<ALGeofenceLocation[]>() {
                    @Override
                    public void onResponse(ALGeofenceLocation[] arrayGeofences) {
                        List<ALGeofenceLocation> alGeofenceLocations = new ArrayList<>();
                        alGeofenceLocations.addAll(Arrays.asList(arrayGeofences));
                        GeofenceController.getInstance().setALGeofenceLocations(alGeofenceLocations);

                        _allGeofencesAdapter = new AllGeofencesAdapter(GeofenceController.getInstance().getALGeofenceLocations());
                        _viewHolder.geofenceRecyclerView.setAdapter(_allGeofencesAdapter);
                        refresh();
                        _allGeofencesAdapter.setListener(new AllGeofencesAdapter.AllGeofencesAdapterListener() {
                            @Override
                            public void onDeleteTapped(final ALGeofenceLocation alGeofenceLocation) {

                                ALLocationManager.deleteGeofenceLocation(alGeofenceLocation, new Response.Listener<AylaAPIRequest.EmptyResponse>() {
                                    @Override
                                    public void onResponse(AylaAPIRequest.EmptyResponse response) {
                                        List<ALGeofenceLocation> ALGeofenceLocations = new ArrayList<>();
                                        ALGeofenceLocations.add(alGeofenceLocation);
                                        GeofenceController.getInstance().removeGeofences(ALGeofenceLocations, geofenceControllerListener);
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
            }
        };

        android.os.Handler h = new android.os.Handler();
        h.post(r);

        _viewHolder.actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _dialogFragment = new AddGeofenceFragment();
                _dialogFragment.setListener(AllGeofencesFragment.this);
                _dialogFragment.show(getActivity().getSupportFragmentManager(), "AddGeofenceFragment");
            }
        });
    }

    private ArrayList<ALGeofenceLocation> loadGeofences() {
        final ArrayList<ALGeofenceLocation> locationArrayList = new ArrayList<>();
        ALLocationManager.fetchGeofenceLocations(new Response.Listener<ALGeofenceLocation[]>() {
            @Override
            public void onResponse(ALGeofenceLocation[] response) {
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

    public void addGeofence(final ALGeofenceLocation geofence) {
        ALLocationManager.addGeofenceLocation(geofence, new Response.Listener<AylaAPIRequest.EmptyResponse>() {
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
