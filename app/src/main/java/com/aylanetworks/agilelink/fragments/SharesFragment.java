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
import android.widget.Toast;

import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaShare;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.fragments.adapters.ShareListAdapter;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.DeviceManager;
import com.aylanetworks.agilelink.framework.SessionManager;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;


public class SharesFragment extends Fragment implements AdapterView.OnItemClickListener, ShareDevicesFragment.ShareDevicesListener {
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
        ShareDevicesFragment dlg = ShareDevicesFragment.newInstance(this);
        dlg.show(getFragmentManager(), "dlg");
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
        String email = amOwner ? share.userProfile.email : share.ownerProfile.email;
        Device device = SessionManager.deviceManager().deviceByDSN(share.resourceId);
        String deviceName = "[unknown device]";
        if ( device != null ) {
            deviceName = device.toString();
        }

        int messageID = amOwner ? R.string.confirm_remove_share_message : R.string.confirm_remove_shared_device_message;
        String formattedMessage = getActivity().getString(messageID, deviceName, email);
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

    @Override
    public void shareDevices(String email, Calendar startDate, Calendar endDate, boolean readOnly, List<Device> devicesToShare) {
        // We got this call from the ShareDevicesFragment.
        if ( devicesToShare != null && !devicesToShare.isEmpty() ) {
            AddSharesHandler handler = new AddSharesHandler(this, email, startDate, endDate, readOnly, devicesToShare);
            MainActivity.getInstance().showWaitDialog(R.string.creating_share_title, R.string.creating_share_body);
            handler.addNextShare();
        }
    }

    private static class AddSharesHandler extends Handler {
        private WeakReference<SharesFragment> _frag;
        private List<Device> _devicesToAdd;
        private String _email;
        private Calendar _startDate;
        private Calendar _endDate;
        private boolean _readOnly;

        private static final DateFormat _dateFormat;
        static {
            _dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        }

        public AddSharesHandler(SharesFragment frag,
                                String email,
                                Calendar startDate,
                                Calendar endDate,
                                boolean readOnly,
                                List<Device> devicesToAdd) {
            _frag = new WeakReference<SharesFragment>(frag);
            _email = email;
            _devicesToAdd = devicesToAdd;
            _startDate = startDate;
            _endDate = endDate;
            _readOnly = readOnly;
        }

        public void addNextShare() {
            if ( _devicesToAdd.isEmpty() ) {
                MainActivity.getInstance().dismissWaitDialog();
                _frag.get().fetchShares();
                return;
            }

            Device device = _devicesToAdd.remove(0);
            AylaShare share = new AylaShare();
            share.userEmail = _email;
            if ( _startDate != null ) {
                share.startDateAt = _dateFormat.format(_startDate.getTime());
            }
            if ( _endDate != null ) {
                share.endDateAt = _dateFormat.format(_endDate.getTime());
            }
            share.operation = _readOnly ? "read" : "write";

            device.getDevice().createShare(this, share);
        }

        @Override
        public void handleMessage(Message msg) {
            if ( AylaNetworks.succeeded(msg) ) {
                addNextShare();
            } else {
                Log.e(LOG_TAG, "Add share failed: " + msg);
                MainActivity.getInstance().dismissWaitDialog();
                String message = MainActivity.getInstance().getString(R.string.error_creating_share);
                if ( msg.obj != null ) {
                    message = (String)msg.obj;
                }
                Toast.makeText(MainActivity.getInstance(), message, Toast.LENGTH_LONG).show();
            }
        }
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
            if ( AylaNetworks.succeeded(msg) ) {
                _frag.get().fetchShares();
            } else {
                String message = MainActivity.getInstance().getString(R.string.error_deleting_share);
                if ( msg.obj != null ) {
                    message = (String)msg.obj;
                }
                Toast.makeText(MainActivity.getInstance(), message, Toast.LENGTH_LONG).show();
            }
        }
    }
}
