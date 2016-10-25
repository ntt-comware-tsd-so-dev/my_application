package com.aylanetworks.agilelink.geofence;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.geofence.GeofenceLocation;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;
import java.util.UUID;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;

/*
 * AMAP_Android
 *
 * Copyright 2016 Ayla Networks, all rights reserved
 */

public class AddGeofenceFragment extends DialogFragment implements ActivityCompat
        .OnRequestPermissionsResultCallback, OnMapReadyCallback {
    private GoogleApiClient _apiClient;
    private EditText _locationAddress;
    private EditText _geofenceName;
    private String _latitude;
    private String _longitude;
    private float _progress = 0;
    private AllGeofencesFragment _geofenceListener;
    private GoogleMap _googleMap;
    private LinearLayout _mapLayout;
    private LatLng _latLng;
    private static final int REQUEST_LOCATION = 0;
    private static final float MINIMAL_RADIUS =50;

    public void setListener(AllGeofencesFragment listener) {
        this._geofenceListener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.add_geofence, null);
        GeofenceController.getInstance().init(this.getActivity());

        _apiClient = new GoogleApiClient
                .Builder(getActivity())
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .build();

        ImageButton geofenceButton = (ImageButton) view.findViewById(R.id.button_add_geofence);
        geofenceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
                try {
                    getActivity().startActivityForResult(builder.build(_geofenceListener.getActivity()),
                            MainActivity.PLACE_PICKER_REQUEST);
                } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
                    e.printStackTrace();
                }
            }
        });
        _geofenceName = (EditText) view.findViewById(R.id.geofence_name);
        _locationAddress = (EditText) view.findViewById(R.id.location_address);

        _locationAddress.setVisibility(View.GONE);
        _mapLayout = (LinearLayout) view.findViewById(R.id.map_layout);
        _mapLayout.setVisibility(View.GONE);
        final TextView textView = (TextView) view.findViewById(R.id.radius_text);
        final SeekBar seekBar = (SeekBar) view.findViewById(R.id.radius_seekbar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
                //The total progress is 500 ft.
                float pvalue = new Double(progressValue * 4.5).floatValue();
                _progress = MINIMAL_RADIUS + pvalue;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                StringBuilder result = new StringBuilder();
                result.append(getString(R.string.geofence_radius));
                result.append(" ");
                result.append(_progress);
                result.append(getString(R.string.radius_units));
                textView.setText(result.toString());
                getActivity().getSupportFragmentManager().findFragmentById(R.id.map);
                DrawMapCircle(_progress);
            }
        });
        Button saveButton = (Button) view.findViewById(R.id.button_save_geofence);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dataIsValid()) {
                    GeofenceLocation geofence = new GeofenceLocation();
                    String uuid = UUID.randomUUID().toString();
                    geofence.setId(uuid);
                    geofence.setName(_geofenceName.getText().toString());
                    geofence.setLatitude(Double.parseDouble(_latitude));
                    geofence.setLongitude(Double.parseDouble(_longitude));
                    geofence.setRadius(_progress);
                    _geofenceListener.addGeofence(geofence);
                    dismiss();
                } else {
                    Toast.makeText(MainActivity.getInstance(), R.string.geofence_validation,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);


        final AlertDialog dialog = builder.create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                positiveButton.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        if (dataIsValid()) {
                            GeofenceLocation geofence = new GeofenceLocation();
                            if (_geofenceListener != null) {
                                _geofenceListener.addGeofence(geofence);
                                dialog.dismiss();
                            }
                        } else {
                            Toast.makeText(MainActivity.getInstance(), R.string.geofence_validation,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });

            }
        });

        if (ContextCompat.checkSelfPermission(getContext(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            LocationManager locationManager =
                    (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_COARSE);
            Location currentLocation;
            List<String> locationProviders = locationManager.getAllProviders();
            for (String provider : locationProviders) {
                currentLocation = locationManager.getLastKnownLocation(provider);
                if (currentLocation != null) {
                    _latitude = (String.valueOf(currentLocation.getLatitude()));
                    _longitude = (String.valueOf(currentLocation.getLongitude()));
                    SupportMapFragment mapFragment =
                            (SupportMapFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.map);
                    mapFragment.getMapAsync(this);
                    break;
                }
            }

        } else {
            requestPermissions(new String[]{ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);
        }

        return dialog;
    }

    private boolean dataIsValid() {
        boolean validData = true;

        String name = _geofenceName.getText().toString();
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(_latitude)
                || TextUtils.isEmpty(_longitude)) {
            validData = false;
        } else {
            double latitude = Double.parseDouble(_latitude);
            double longitude = Double.parseDouble(_longitude);
            float radius = _progress;
            if ((latitude < GeofenceLocation.Geometry.MinLatitude || latitude > GeofenceLocation.Geometry.MaxLatitude)
                    || (longitude < GeofenceLocation.Geometry.MinLongitude || longitude > GeofenceLocation.Geometry.MaxLongitude)
                    || (radius < GeofenceLocation.Geometry.MinRadius || radius > GeofenceLocation.Geometry.MaxRadius)) {
                validData = false;
            }
        }
        return validData;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MainActivity.PLACE_PICKER_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                Place place = PlacePicker.getPlace(getActivity(), data);
                StringBuilder stBuilder = new StringBuilder();
                _latitude = String.valueOf(place.getLatLng().latitude);
                _longitude = String.valueOf(place.getLatLng().longitude);
                String address = String.format("%s", place.getAddress());
                stBuilder.append(address);
                _locationAddress.setText(stBuilder.toString());
                _locationAddress.setVisibility(View.VISIBLE);
                _progress = 50;//Initial value
                SupportMapFragment mapFragment =
                        (SupportMapFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.map);
                mapFragment.getMapAsync(this);
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            dialog.getWindow().setLayout(width, height);
        }
        _apiClient.connect();
    }

    @Override
    public void onStop() {
        _apiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        _googleMap = map;
        _latLng = new LatLng(Double.parseDouble(_latitude), Double.parseDouble(_longitude));
        _googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(_latLng, 15.0f));
        new DraggableCircle(_latLng, _progress);
        _mapLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        SupportMapFragment mapFragment =
                (SupportMapFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null)
            getFragmentManager().beginTransaction().remove(mapFragment).commit();
    }

    public void setAddress(String address) {
        _locationAddress.setText(address);
    }

    private void DrawMapCircle(double radius) {
        _googleMap.clear();
        new DraggableCircle(_latLng, radius);
    }

    private class DraggableCircle {
        public DraggableCircle(LatLng center, double radius) {
            _googleMap.addMarker(new MarkerOptions()
                    .position(center)
                    .draggable(true));
            _googleMap.addCircle(new CircleOptions()
                    .center(center)
                    .radius(radius)
                    .strokeWidth(2f)
                    //.strokeColor(mStrokeColor)
                    .fillColor(0x550000FF));
        }
    }
}
