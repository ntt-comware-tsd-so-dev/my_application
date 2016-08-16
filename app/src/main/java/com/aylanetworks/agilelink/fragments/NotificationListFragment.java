package com.aylanetworks.agilelink.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.aylanetworks.agilelink.ErrorUtils;
import com.aylanetworks.agilelink.framework.AMAPCore;
import com.aylanetworks.agilelink.framework.PropertyNotificationHelper.FetchNotificationsListener;
import com.aylanetworks.agilelink.framework.ViewModel;
import com.aylanetworks.aylasdk.AylaAPIRequest;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaLog;
import com.aylanetworks.aylasdk.AylaProperty;
import com.aylanetworks.aylasdk.AylaPropertyTrigger;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.PropertyNotificationHelper;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
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

    public static NotificationListFragment newInstance(ViewModel deviceModel) {
        NotificationListFragment frag = new NotificationListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DSN, deviceModel.getDevice().getDsn());
        frag.setArguments(args);
        return frag;
    }

    public NotificationListFragment() {}

    private RecyclerView _recyclerView;
    private TextView _emptyView;
    private ViewModel _deviceModel;
    private List<AylaPropertyTrigger> _propertyTriggers;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AylaDevice device = AMAPCore.sharedInstance().getDeviceManager()
                .deviceWithDSN(getArguments().getString(ARG_DSN));
        _deviceModel = AMAPCore.sharedInstance().getSessionParameters().viewModelProvider
                .viewModelForDevice(device);
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
        PropertyNotificationHelper propertyNotificationHelper = new PropertyNotificationHelper(_deviceModel.getDevice());

        MainActivity.getInstance().showWaitDialog(R.string.please_wait, R.string.please_wait);
        propertyNotificationHelper.fetchNotifications(new FetchNotificationsListener() {
            @Override
            public void notificationsFetched(AylaDevice device, AylaError error) {
                MainActivity.getInstance().dismissWaitDialog();
                Log.d(LOG_TAG, "notificationsFetched: " + error);

                updateTriggersData();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void updateTriggersData() {
        // Gather all of the property triggers
        _propertyTriggers = new ArrayList<>();
        if ( _deviceModel.getDevice().getProperties() == null ) {
            Log.e(LOG_TAG, "No properties found on device");
            Toast.makeText(getActivity(), R.string.unknown_error, Toast.LENGTH_LONG).show();

            getFragmentManager().popBackStack();
            return;
        }

        final String[] propNames = _deviceModel.getNotifiablePropertyNames();
        for (String propName : propNames) {
            AylaProperty aylaProperty = _deviceModel.getDevice().getProperty(propName);
            if (aylaProperty == null) {
                AylaLog.e(LOG_TAG, "No property returned for " + propName);
                continue;
            }

            aylaProperty.fetchTriggers(
                    new Response.Listener<AylaPropertyTrigger[]>() {
                       @Override
                       public void onResponse(AylaPropertyTrigger[] response) {
                           if(response != null && response.length > 0) {
                               _propertyTriggers.addAll(Arrays.asList(response));
                           }

                           updateTriggersList();
                       }
                   },
                    new ErrorListener() {
                        @Override
                        public void onErrorResponse(AylaError error) {
                            Toast.makeText(getContext(),
                                    ErrorUtils.getUserMessage(getActivity(), error, R.string.unknown_error),
                                    Toast.LENGTH_LONG).show();

                            updateTriggersList();
                        }
                    });
        }
    }

    private void updateTriggersList() {
        if ( _propertyTriggers.isEmpty() ) {
            _recyclerView.setVisibility(View.GONE);
            _emptyView.setVisibility(View.VISIBLE);
            _emptyView.setText(R.string.no_triggers_found);
        } else {
            _recyclerView.setVisibility(View.VISIBLE);
            _emptyView.setVisibility(View.GONE);
        }
        _recyclerView.setAdapter(new TriggerAdapter(NotificationListFragment.this, _deviceModel, _propertyTriggers));
    }

    @Override
    public void onClick(View v) {
        // Add button tapped
        PropertyNotificationFragment frag = PropertyNotificationFragment.newInstance(_deviceModel
                .getDevice(), null);
        MainActivity.getInstance().pushFragment(frag);
    }

    private void onLongClick(final int index) {
        AylaPropertyTrigger trigger = _propertyTriggers.get(index);
        new AlertDialog.Builder(getActivity())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.delete_notification_title)
                .setMessage(getActivity().getResources().getString(R.string
                        .delete_notification_message, trigger.getDeviceNickname()))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AylaPropertyTrigger trigger = _propertyTriggers.get(index);
                        String propNickName = trigger.getPropertyNickname();
                        AylaProperty property = _deviceModel.getDevice().getProperty(propNickName);
                        if (property == null) {
                            String error = "No property returned for " + propNickName;
                            AylaLog.e(LOG_TAG, error);
                            Toast.makeText(MainActivity.getInstance(), error, Toast.LENGTH_LONG).show();
                            return;
                        }
                        property.deleteTrigger(trigger, new Response.Listener<AylaAPIRequest.EmptyResponse>() {
                                    @Override
                                    public void onResponse(AylaAPIRequest.EmptyResponse response) {
                                        AylaLog.d(LOG_TAG, "Successfully Deleted the old trigger");
                                        updateTriggersData();
                                    }
                                },
                                new ErrorListener() {
                                    @Override
                                    public void onErrorResponse(AylaError error) {
                                        Toast.makeText(getContext(),
                                                ErrorUtils.getUserMessage(getActivity(), error, R.string.unknown_error),
                                                Toast.LENGTH_LONG).show();
                                    }
                                });
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .create().show();
    }

    private static class TriggerAdapter extends RecyclerView.Adapter {
        private WeakReference<NotificationListFragment> _frag;
        private List<AylaPropertyTrigger>_propertyTriggers;
        private ViewModel _deviceModel;

        public TriggerAdapter(NotificationListFragment fragment, ViewModel deviceModel,
                              List<AylaPropertyTrigger> propertyTriggers) {
            _frag = new WeakReference<>(fragment);
            _deviceModel = deviceModel;
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
            h._triggerName.setText(trigger.getDeviceNickname());
            h._propertyName.setText(_deviceModel.friendlyNameForPropertyName(trigger.getPropertyNickname()));
            h.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MainActivity.getInstance().pushFragment(PropertyNotificationFragment
                            .newInstance(_deviceModel.getDevice(), trigger));
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
