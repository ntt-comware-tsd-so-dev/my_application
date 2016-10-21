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
import com.aylanetworks.agilelink.framework.geofence.ALGeofenceLocation;

import java.util.List;

/*
 * AMAP_Android
 *
 * Copyright 2016 Ayla Networks, all rights reserved
 */

public class AllGeofencesAdapter extends RecyclerView.Adapter<AllGeofencesAdapter.ViewHolder> {

    private final List<ALGeofenceLocation> _alGeofenceLocations;
    private AllGeofencesAdapterListener _listener;

    public void setListener(AllGeofencesAdapterListener _listener) {
        this._listener = _listener;
    }

    public AllGeofencesAdapter(List<ALGeofenceLocation> _alGeofenceLocations) {
        this._alGeofenceLocations = _alGeofenceLocations;
    }

    @Override
    public AllGeofencesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewGroup v = (ViewGroup) LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_geofence, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final ALGeofenceLocation geofence = _alGeofenceLocations.get(position);
        holder.name.setText(geofence.getName());

        holder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.getInstance())
                        .setIcon(R.drawable.ic_launcher)
                        .setTitle(R.string.AreYouSure)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (_listener != null) {
                                    _listener.onDeleteTapped(geofence);
                                }
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .create().show();
            }
        });
    }

    @Override
    public int getItemCount() {
        if (_alGeofenceLocations == null) {
            return 0;
        }
        return _alGeofenceLocations.size();
    }

    protected class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final TextView name;
        final Button deleteButton;

        public ViewHolder(ViewGroup v) {
            super(v);
            name = (TextView) v.findViewById(R.id.listitem_geofenceName);
            deleteButton = (Button) v.findViewById(R.id.listitem_deleteButton);
        }

        @Override
        public void onClick(View v) {
        }
    }

    public interface AllGeofencesAdapterListener {
        void onDeleteTapped(ALGeofenceLocation ALGeofenceLocation);
    }
}

