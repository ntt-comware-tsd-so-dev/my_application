/*
 * SharesFragment.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 3/26/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

package com.aylanetworks.agilelink.fragments;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.Response;
import com.aylanetworks.agilelink.framework.AMAPCore;
import com.aylanetworks.aylasdk.AylaAPIRequest;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaLog;
import com.aylanetworks.aylasdk.AylaShare;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.fragments.adapters.ShareListAdapter;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;


public class SharesFragment extends Fragment implements AdapterView.OnItemClickListener, ShareDevicesFragment.ShareDevicesListener {
    private final static String LOG_TAG = "SharesFragment";

    private ListView _listViewSharedByMe;
    private ListView _listViewSharedToMe;
    private AylaShare[] _ownedShares;
    private AylaShare[] _receivedShares;
    private ShareListAdapter _ownedShareAdapter;
    private ShareListAdapter _receivedShareAdapter;

    public static SharesFragment newInstance() {
        return new SharesFragment();
    }

    public SharesFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_shares, container, false);

        _listViewSharedByMe = (ListView)root.findViewById(R.id.listview_devices_i_share);
        _listViewSharedByMe.setOnItemClickListener(this);
        _listViewSharedByMe.setEmptyView(root.findViewById(R.id.my_shares_empty));

        _listViewSharedToMe = (ListView)root.findViewById(R.id.listview_devices_shared_with_me);
        _listViewSharedToMe.setOnItemClickListener(this);
        _listViewSharedToMe.setEmptyView(root.findViewById(R.id.shared_with_me_empty));

        ImageButton addButton = (ImageButton)root.findViewById(R.id.add_button);

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addTapped();
            }
        });

        fetchShares();

        return root;
    }

    private void fetchShares(){
        AMAPCore.sharedInstance().getSessionManager().fetchOwnedShares(new Response.Listener<AylaShare[]>() {
            @Override
            public void onResponse(AylaShare[] response) {
                _ownedShares = response;
                _ownedShareAdapter.clear();
                _ownedShareAdapter.addAll(_ownedShares);
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                AylaLog.e(LOG_TAG, "Error in fetching shares "+ error.getLocalizedMessage());
            }
        });

        AMAPCore.sharedInstance().getSessionManager().fetchReceivedShares(new Response
                .Listener<AylaShare[]>() {
            @Override
            public void onResponse(AylaShare[] response) {
                _receivedShares = response;
                _receivedShareAdapter.clear();
                _receivedShareAdapter.addAll(_receivedShares);
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                AylaLog.e(LOG_TAG, "Error in fetching shares "+ error.getLocalizedMessage());
            }
        });

        _ownedShareAdapter.notifyDataSetChanged();
        _receivedShareAdapter.notifyDataSetChanged();
    }

    private void addTapped() {
        Log.d(LOG_TAG, "Add button tapped");
        ShareDevicesFragment frag = ShareDevicesFragment.newInstance(this);
        MainActivity.getInstance().pushFragment(frag);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if ( parent == _listViewSharedByMe ) {
            AylaShare share = (AylaShare)_listViewSharedByMe.getAdapter().getItem(position);
            Log.d(LOG_TAG, "Share clicked: " + share);
            confirmRemoveShare(share, true);
        } else {
            AylaShare share = (AylaShare)_listViewSharedToMe.getAdapter().getItem(position);
            Log.d(LOG_TAG, "Share clicked: " + share);
            confirmRemoveShare(share, false);
        }
    }

    private void confirmRemoveShare(final AylaShare share, boolean amOwner) {
        String email = amOwner ? share.getUserProfile().email : share.getOwnerProfile().email;
        AylaDevice device = AMAPCore.sharedInstance().getDeviceManager().deviceWithDSN(share.getResourceId());
        String deviceName = "[unknown device]";
        if ( device != null ) {
            deviceName = device.getDeviceName();
        }

        int messageID = amOwner ? R.string.confirm_remove_share_message : R.string.confirm_remove_shared_device_message;
        String formattedMessage = getActivity().getString(messageID, deviceName, email);
        new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.ic_launcher)
                .setTitle(R.string.confirm_remove_share_title)
                .setMessage(formattedMessage)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(LOG_TAG, "Removing share " + share);
                        MainActivity.getInstance().showWaitDialog(R.string.removing_share_title, R.string.removing_share_body);
                        AMAPCore.sharedInstance().getSessionManager().deleteShare(share.getId(),
                                new Response.Listener<AylaAPIRequest.EmptyResponse>() {
                            @Override
                            public void onResponse(AylaAPIRequest.EmptyResponse response) {
                                fetchShares();
                            }
                        }, new ErrorListener() {
                            @Override
                            public void onErrorResponse(AylaError error) {
                                Toast.makeText(MainActivity.getInstance(), error.getMessage(), Toast
                                        .LENGTH_LONG).show();
                            }
                        });
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .create().show();
    }

    @Override
    public void shareDevices(String email, String role, Calendar startDate, Calendar endDate,
                             boolean readOnly, List<AylaDevice> devicesToShare) {
        // We got this call from the ShareDevicesFragment.
        getFragmentManager().popBackStack();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        if ( devicesToShare != null && !devicesToShare.isEmpty() ) {
          //  AddSharesHandler handler = new AddSharesHandler(this, email, role, startDate, endDate,
          ///          readOnly, devicesToShare);
            MainActivity.getInstance().showWaitDialog(R.string.creating_share_title, R.string.creating_share_body);
            for(AylaDevice device:devicesToShare){
                String strStartDate= dateFormat.format(startDate.getTime());
                String strEndDate =  dateFormat.format(endDate.getTime());
                AylaShare share = device.shareWithEmail(email, "read", role, strStartDate, strEndDate );
                AMAPCore.sharedInstance().getSessionManager().createShare(share,null,new Response.Listener<AylaShare>(){
                            @Override
                            public void onResponse(AylaShare response) {
                            }
                        },
                        new ErrorListener() {
                            @Override
                            public void onErrorResponse(AylaError error) {
                                Toast.makeText(MainActivity.getInstance(), error.getMessage(), Toast
                                        .LENGTH_LONG).show();
                            }
                        });
            }


        }
    }
}
