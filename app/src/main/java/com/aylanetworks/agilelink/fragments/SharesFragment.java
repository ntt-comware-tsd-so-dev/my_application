/*
 * SharesFragment.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 3/26/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

package com.aylanetworks.agilelink.fragments;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;

import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaShare;
import com.aylanetworks.aaml.AylaUser;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.fragments.adapters.ShareListAdapter;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.DeviceManager;
import com.aylanetworks.agilelink.framework.SessionManager;

import java.lang.ref.WeakReference;


public class SharesFragment extends Fragment implements AdapterView.OnItemClickListener {
    private final static String LOG_TAG = "SharesFragment";

    private ListView _listViewSharedByMe;
    private ListView _listViewSharedToMe;

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

        _listViewSharedToMe = (ListView)root.findViewById(R.id.listview_devices_shared_with_me);
        _listViewSharedToMe.setOnItemClickListener(this);

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

    private void fetchShares() {
        DeviceManager.FetchSharesListener listener = new DeviceManager.FetchSharesListener(true, true) {
            @Override
            public void sharesFetched(boolean successful) {
                MainActivity.getInstance().dismissWaitDialog();
                if ( successful ) {
                    _listViewSharedToMe.setAdapter(new ShareListAdapter(getActivity(), receivedShares));
                    _listViewSharedByMe.setAdapter(new ShareListAdapter(getActivity(), ownedShares));
                }
            }
        };

        MainActivity.getInstance().showWaitDialog(R.string.fetching_shares_title, R.string.fetching_shares_body);
        SessionManager.deviceManager().fetchShares(listener);
    }

    private void addTapped() {
        Log.d(LOG_TAG, "Add button tapped");
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

        String email = amOwner ? share.ownerProfile.email : share.userProfile.email;
        Device device = SessionManager.deviceManager().deviceByDSN(share.resourceId);
        String deviceName = "[unknown device]";
        if ( device != null ) {
            deviceName = device.toString();
        }

        int messageID = amOwner ? R.string.confirm_remove_share_message : R.string.confirm_remove_shared_device_message;
        String formattedMessage = getActivity().getString(R.string.confirm_remove_share_message, deviceName, email);
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.confirm_remove_share_title)
                .setMessage(formattedMessage)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(LOG_TAG, "Removing share " + share);
                        MainActivity.getInstance().showWaitDialog(R.string.removing_share_title, R.string.removing_share_body);
                        share.delete(new DeleteShareHandler(SharesFragment.this));
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .create().show();
    }

    private static class DeleteShareHandler extends Handler {
        private WeakReference<SharesFragment> _frag;

        public DeleteShareHandler(SharesFragment frag) {
            _frag = new WeakReference<SharesFragment>(frag);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity.getInstance().dismissWaitDialog();
            Log.d(LOG_TAG, "Delete share: " + msg);
            if ( msg.what == AylaNetworks.AML_ERROR_OK ) {
                _frag.get().fetchShares();
            }
        }
    }
}
