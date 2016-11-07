package com.aylanetworks.agilelink.geofence;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.geofence.GeofenceLocation;

import java.util.List;

/*
 * AMAP_Android
 *
 * Copyright 2016 Ayla Networks, all rights reserved
 */

public class AllGeofencesAdapter extends RecyclerView.Adapter<AllGeofencesAdapter.ViewHolder> {

    private final List<GeofenceLocation> _geofenceLocations;

    public AllGeofencesAdapter(List<GeofenceLocation> _GeofenceLocations) {
        this._geofenceLocations = _GeofenceLocations;
    }

    @Override
    public AllGeofencesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewGroup v = (ViewGroup) LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_geofence, parent, false);
        return new ViewHolder(v);
    }

    public List<GeofenceLocation> getGeofenceLocations() {
        return _geofenceLocations;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final GeofenceLocation geofence = _geofenceLocations.get(position);
        holder.name.setText(geofence.getName());
        /*holder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.getInstance())
                        .setIcon(R.drawable.ic_launcher)
                        .setTitle(R.string.confirm_delete_geofence)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (_listener != null) {
                                    _listener.onDeleteTapped(geofence);
                                }
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .create().show();*
            }
        });*/
    }

    @Override
    public int getItemCount() {
        if (_geofenceLocations == null) {
            return 0;
        }
        return _geofenceLocations.size();
    }

    protected class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final TextView name;

        public ViewHolder(ViewGroup v) {
            super(v);
            name = (TextView) v.findViewById(R.id.listitem_geofenceName);
        }

        @Override
        public void onClick(View v) {
        }
    }
}

