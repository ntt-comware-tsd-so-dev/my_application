package com.aylanetworks.agilelink.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaProperty;
import com.aylanetworks.aaml.AylaPropertyTrigger;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.PropertyNotificationHelper;
import com.aylanetworks.agilelink.framework.SessionManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * NotificationListFragment.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 5/7/2015.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */
public class NotificationListFragment extends Fragment implements View.OnClickListener {
    private static final String ARG_DSN = "dsn";
    private static final String LOG_TAG = "NotListFrag";

    public static NotificationListFragment newInstance(Device device) {
        NotificationListFragment frag = new NotificationListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DSN, device.getDevice().dsn);
        frag.setArguments(args);
        return frag;
    }

    public NotificationListFragment() {}

    private RecyclerView _recyclerView;
    private TextView _emptyView;
    private Device _device;
    private PropertyNotificationHelper _propertyNotificationHelper;
    private List<AylaPropertyTrigger> _propertyTriggers;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _device = SessionManager.deviceManager().deviceByDSN(getArguments().getString(ARG_DSN));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_all_devices, container, false);
        _emptyView = (TextView) view.findViewById(R.id.empty);

        // Set up the list view

        _recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        _recyclerView.setHasFixedSize(true);

        _recyclerView.setVisibility(View.GONE);
        _emptyView.setVisibility(View.VISIBLE);
        _emptyView.setText(R.string.fetching_notifications);

        RecyclerView.LayoutManager lm  = new LinearLayoutManager(getActivity());
        _recyclerView.setLayoutManager(lm);

        ImageButton b = (ImageButton) view.findViewById(R.id.add_button);
        b.setOnClickListener(this);

        // Get the notifications
        _propertyNotificationHelper = new PropertyNotificationHelper(_device);
        MainActivity.getInstance().showWaitDialog(R.string.please_wait, R.string.please_wait);
        _propertyNotificationHelper.fetchNotifications(new Device.FetchNotificationsListener() {
            @Override
            public void notificationsFetched(Device device, boolean succeeded) {
                MainActivity.getInstance().dismissWaitDialog();
                Log.d(LOG_TAG, "notificationsFetched: " + succeeded);

                updateTriggerList();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        SessionManager.deviceManager().stopPolling();
    }

    private void updateTriggerList() {
        // Gather all of the property triggers
        _propertyTriggers = new ArrayList<>();
        if ( _device.getDevice().properties == null ) {
            Log.e(LOG_TAG, "No properties found on device");
            Toast.makeText(getActivity(), R.string.unknown_error, Toast.LENGTH_LONG).show();

            getFragmentManager().popBackStack();
            return;
        }
        for (AylaProperty prop : _device.getDevice().properties) {
            if ( prop.propertyTriggers != null && prop.propertyTriggers.length > 0 ) {
                for ( AylaPropertyTrigger trigger : prop.propertyTriggers ) {
                    _propertyTriggers.add(trigger);
                }
            }
        }

        if ( _propertyTriggers.isEmpty() ) {
            _recyclerView.setVisibility(View.GONE);
            _emptyView.setVisibility(View.VISIBLE);
            _emptyView.setText(R.string.no_triggers_found);
        } else {
            _recyclerView.setVisibility(View.VISIBLE);
            _emptyView.setVisibility(View.GONE);
        }
        _recyclerView.setAdapter(new TriggerAdapter(this, _device, _propertyTriggers));
    }

    @Override
    public void onClick(View v) {
        // Add button tapped
        PropertyNotificationFragment frag = PropertyNotificationFragment.newInstance(_device, null);
        MainActivity.getInstance().pushFragment(frag);
    }

    private void onLongClick(final int index) {
        AylaPropertyTrigger trigger = _propertyTriggers.get(index);
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.delete_notification_title)
                .setMessage(getActivity().getResources().getString(R.string.delete_notification_message, trigger.deviceNickname))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AylaPropertyTrigger trigger = _propertyTriggers.get(index);
                        MainActivity.getInstance().showWaitDialog(R.string.please_wait, R.string.please_wait);
                        trigger.destroyTrigger(new DeleteTriggerHandler(NotificationListFragment.this, index));
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .create().show();
    }

    private static class DeleteTriggerHandler extends Handler {
        private WeakReference<NotificationListFragment> _frag;
        private int _index;

        public DeleteTriggerHandler(NotificationListFragment frag, int index) {
            _frag = new WeakReference<NotificationListFragment>(frag);
            _index = index;
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity.getInstance().dismissWaitDialog();
            if (AylaNetworks.succeeded(msg)) {
                _frag.get()._propertyTriggers.remove(_index);
                _frag.get()._recyclerView.getAdapter().notifyItemRemoved(_index);
            } else {
                Toast.makeText(_frag.get().getActivity(), (String)msg.obj, Toast.LENGTH_LONG).show();
            }
        }
    }

    private static class TriggerAdapter extends RecyclerView.Adapter {
        private WeakReference<NotificationListFragment> _frag;
        private List<AylaPropertyTrigger>_propertyTriggers;
        private Device _device;

        public TriggerAdapter(NotificationListFragment fragment, Device device, List<AylaPropertyTrigger> propertyTriggers) {
            _frag = new WeakReference<NotificationListFragment>(fragment);
            _device = device;
            _propertyTriggers = propertyTriggers;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_property_trigger, parent, false);
            return new TriggerViewHolder(v);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
            final AylaPropertyTrigger trigger = _propertyTriggers.get(position);
            TriggerViewHolder h = (TriggerViewHolder)holder;
            h._triggerName.setText(trigger.deviceNickname);
            h._propertyName.setText(_device.friendlyNameForPropertyName(trigger.propertyNickname));
            h.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MainActivity.getInstance().pushFragment(PropertyNotificationFragment.newInstance(_device, trigger));
                }
            });
            h.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    _frag.get().onLongClick(position);
                    return true;
                }
            });
        }

        @Override
        public int getItemCount() {
            return _propertyTriggers.size();
        }
    }

    private static class TriggerViewHolder extends RecyclerView.ViewHolder {
        private TextView _triggerName;
        private TextView _propertyName;

        public TriggerViewHolder(View v) {
            super(v);
            _triggerName = (TextView)v.findViewById(R.id.trigger_name);
            _propertyName = (TextView)v.findViewById(R.id.trigger_property);
        }
    }

}
