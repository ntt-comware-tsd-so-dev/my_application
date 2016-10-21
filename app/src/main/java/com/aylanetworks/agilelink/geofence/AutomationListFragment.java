package com.aylanetworks.agilelink.geofence;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.geofence.ALAutomation;
import com.aylanetworks.agilelink.framework.geofence.ALAutomationManager;
import com.aylanetworks.aylasdk.AylaAPIRequest;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;

import java.util.ArrayList;
import java.util.Arrays;

/*
 * AMAP_Android
 *
 * Copyright 2016 Ayla Networks, all rights reserved
 */

public class AutomationListFragment extends Fragment implements AdapterView.OnItemLongClickListener,
        AdapterView.OnItemClickListener {
    private final static String LOG_TAG = "AutomationListFragment";

    private ListView _listViewAutomations;
    private ALAutomation[] _alAutomations;
    private AutomationListAdapter _automationsAdapter;

    public static AutomationListFragment newInstance() {
        return new AutomationListFragment();
    }

    public AutomationListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
      ALAutomation alAutomation = _automationsAdapter.getItem(position);
        EditAutomationFragment frag = EditAutomationFragment.newInstance(alAutomation);
        MainActivity.getInstance().pushFragment(frag);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_all_automations, container, false);

        _listViewAutomations = (ListView)root.findViewById(R.id.listViewAutomations);
        _listViewAutomations.setOnItemLongClickListener(this);
        _listViewAutomations.setOnItemClickListener(this);
        _listViewAutomations.setEmptyView(root.findViewById(R.id.automations_empty));
        

        ImageButton addButton = (ImageButton)root.findViewById(R.id.add_button);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addTapped();
            }
        });
        Runnable r = new Runnable() {
            @Override
            public void run() {
                fetchAutomations();
            }
        };

        android.os.Handler h = new android.os.Handler();
        h.post(r);
        return root;
    }

    private void fetchAutomations(){
        ALAutomationManager.fetchAutomation(new Response.Listener<ALAutomation[]>() {
            @Override
            public void onResponse(ALAutomation[] response) {
                _alAutomations = response;
                if(isAdded()){
                    if (_automationsAdapter == null) {
                        _automationsAdapter = new AutomationListAdapter(getContext(), new ArrayList<>(Arrays.asList(response)));
                        _listViewAutomations.setAdapter(_automationsAdapter);
                    } else {
                        _automationsAdapter.clear();
                        _automationsAdapter.addAll(_alAutomations);
                        _listViewAutomations.setAdapter(_automationsAdapter);
                        _automationsAdapter.notifyDataSetChanged();
                    }
                }
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

    private void addTapped() {
        Log.d(LOG_TAG, "Add button tapped");
        EditAutomationFragment frag = EditAutomationFragment.newInstance(null);
        MainActivity.getInstance().pushFragment(frag);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        ALAutomation alAutomation = (ALAutomation)_listViewAutomations.getAdapter().getItem(position);
        confirmRemoveAutomation(alAutomation);
        return true;
    }

    private void confirmRemoveAutomation(final ALAutomation alAutomation) {
             ALAutomationManager.deleteAutomation(alAutomation, new Response.Listener<AylaAPIRequest
                    .EmptyResponse>() {
                @Override
                public void onResponse(AylaAPIRequest.EmptyResponse response) {
                    String msg = "Deleted Successfully";
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

    public class AutomationListAdapter extends ArrayAdapter<ALAutomation> {
        public AutomationListAdapter(Context c, ArrayList<ALAutomation> alAutomations) {
            super(c, android.R.layout.simple_list_item_1, android.R.id.text1, alAutomations);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View v = super.getView(position, convertView, parent);
            TextView tv1 = (TextView)v.findViewById(android.R.id.text1);
            ALAutomation alAutomation = getItem(position);

            if ( alAutomation!=null && alAutomation.getId() != null ) {
                tv1.setText(alAutomation.getName());
            } else {
                tv1.setText("");
            }
            return v;
        }
    }
}


