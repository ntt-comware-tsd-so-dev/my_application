package com.aylanetworks.agilelink.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.aylanetworks.agilelink.framework.AMAPCore;
import com.aylanetworks.agilelink.framework.ViewModel;
import com.aylanetworks.aylasdk.AylaAPIRequest;
import com.aylanetworks.aylasdk.AylaDatapoint;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaDeviceGateway;
import com.aylanetworks.aylasdk.AylaLog;
import com.aylanetworks.aylasdk.AylaProperty;
import com.aylanetworks.aylasdk.AylaShare;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.Logger;
import com.aylanetworks.aylasdk.AylaTimeZone;
import com.aylanetworks.aylasdk.change.Change;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;
import com.aylanetworks.aylasdk.error.RequestFuture;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * DeviceDetailFragment.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 1/15/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class DeviceDetailFragment extends Fragment implements AylaDevice.DeviceChangeListener,
        View.OnClickListener,
        ShareDevicesFragment.ShareDevicesListener {

    public final static String LOG_TAG = "DeviceDetailFragment";

    public final static int FRAGMENT_RESOURCE_ID = R.layout.fragment_device_detail;

    public final static String ARG_DEVICE_DSN = "DeviceDSN";

    private ViewModel _deviceModel;
    private ListView _listView;
    private PropertyListAdapter _adapter;
    private TextView _titleView;
    private TextView _dsnView;
    private ImageView _imageView;
    private Button _scheduleButton;
    private Button _notificationsButton;

    private Context mContext;

    public static DeviceDetailFragment newInstance(ViewModel deviceModel) {
        DeviceDetailFragment frag = new DeviceDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DEVICE_DSN, deviceModel.getDevice().getDsn());
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        _deviceModel = null;
        if (getArguments() != null ) {
            String dsn = getArguments().getString(ARG_DEVICE_DSN);
            AylaDevice d = AMAPCore.sharedInstance().getDeviceManager().deviceWithDSN(dsn);
            _deviceModel = AMAPCore.sharedInstance().getSessionParameters().viewModelProvider
                    .viewModelForDevice(d);
        }
     }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(FRAGMENT_RESOURCE_ID, container, false);

        _listView = (ListView)view.findViewById(R.id.listView);
        _titleView = (TextView)view.findViewById(R.id.device_name);
        _dsnView = (TextView)view.findViewById(R.id.device_dsn);
        _imageView = (ImageView)view.findViewById(R.id.device_image);

        _notificationsButton = (Button)view.findViewById(R.id.notifications_button);
        _notificationsButton.setOnClickListener(this);

        _scheduleButton = (Button)view.findViewById(R.id.schedule_button);
        _scheduleButton.setOnClickListener(this);

        Button sharingButton = (Button)view.findViewById(R.id.sharing_button);
        if(_deviceModel.isNode()){
            sharingButton.setVisibility(View.GONE);
        } else{
            sharingButton.setOnClickListener(this);
        }
        if ( _deviceModel.getDevice().getGrant() != null ) {
            // This device was shared with us
            sharingButton.setVisibility(View.GONE);
            _dsnView.setVisibility(View.GONE);
        } else {
            // This device is ours. Allow the name to be changed.
            _titleView.setTextColor(getResources().getColor(R.color.link));
            _titleView.setPaintFlags(Paint.UNDERLINE_TEXT_FLAG);
            _titleView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    titleClicked();
                }
            });
        }

        updateUI();

        return view;
    }

    private void titleClicked() {
        // Let the user change the title
        final EditText editText = new EditText(getActivity());
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        editText.setText(_deviceModel.toString());
        editText.setSelectAllOnFocus(true);
        editText.requestFocus();

        new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.ic_launcher)
                .setTitle(R.string.rename_device_text)
                .setView(editText)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        changeDeviceName(editText.getText().toString());
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create().show();
        editText.requestFocus();
    }

    @Override
    public void deviceChanged(AylaDevice device, Change change) {
        AylaLog.i(LOG_TAG, "Device changed: " + device + ": " + change);
        updateUI();
    }

    @Override
    public void deviceError(AylaDevice device, AylaError error) {
        AylaLog.e(LOG_TAG, "Device error " + device + " " + error);
        Toast.makeText(MainActivity.getInstance(), error.getMessage(), Toast.LENGTH_LONG).show();
        updateUI();
    }

    @Override
    public void deviceLanStateChanged(AylaDevice device, boolean lanModeEnabled) {
        AylaLog.i(LOG_TAG, "Device " + device + " LAN state chagned: " + lanModeEnabled);
        updateUI();
    }

    private void changeDeviceName(String newDeviceName) {
        Map<String, String> params = new HashMap<>();
        params.put("productName", newDeviceName);
        _deviceModel.getDevice().updateProductName(newDeviceName,
                new Response.Listener<AylaAPIRequest.EmptyResponse>() {
                    @Override
                    public void onResponse(AylaAPIRequest.EmptyResponse response) {
                        updateUI();
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

    void updateUI() {
        if ( _deviceModel == null ) {
            Log.e(LOG_TAG, "Unable to find device!");
            getFragmentManager().popBackStack();
            Toast.makeText(getActivity(), R.string.unknown_error, Toast.LENGTH_SHORT).show();
        } else {
            // Get the property list and set up our adapter
            List<AylaProperty> propertyList = _deviceModel.getDevice().getProperties();
            if(mContext != null) {
                _adapter = new PropertyListAdapter(mContext, propertyList);
            }
        }

        // Can this device set schedules or property notifications?
        _scheduleButton.setVisibility(_deviceModel.getSchedulablePropertyNames().length > 0 ? View.VISIBLE : View.GONE);
        _notificationsButton.setVisibility(_deviceModel.getNotifiablePropertyNames().length > 0 ? View.VISIBLE : View.GONE);

        // Set the device title and image
        _titleView.setText(_deviceModel.toString());
        _dsnView.setText(_deviceModel.isInLanMode() ? _deviceModel.getDevice().getLanIp() :
                _deviceModel.getDevice().getDsn());
        _imageView.setImageDrawable(((ViewModel) _deviceModel).getDeviceDrawable(getActivity()));
        _listView.setAdapter(_adapter);

    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        MainActivity.getInstance().getMenuInflater().inflate(R.menu.menu_device_details, menu);

        boolean hasFactoryReset = false;
        if (_deviceModel != null) {
            // djunod: I have never been able to do Factory Reset on a Zigbee Gateway
            //hasFactoryReset = (_deviceModel.isGateway() || _deviceModel.isDeviceNode());
            hasFactoryReset = _deviceModel.getDevice().isNode();
        }
        menu.getItem(1).setVisible(hasFactoryReset);
        menu.getItem(0).setVisible(!hasFactoryReset);

        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void shareDevices(String email, String role, Calendar startDate, Calendar endDate,
                             boolean readOnly, List<AylaDevice> devicesToShare) {

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String startDateString = null;
        String endDateString = null;
        String roleName = null;

        if ( startDate != null ) {
            startDateString = df.format(startDate.getTime());
        }
        if ( endDate != null ) {
            endDateString = df.format(endDate.getTime());
        }

        AylaShare share = new AylaShare(email, readOnly ? "read" : "write",
                _deviceModel.getDevice().getDsn(),
                _deviceModel.getDevice().getProductName(),
                role, startDateString, endDateString);

        AMAPCore.sharedInstance().getSessionManager().createShare(share, null,
                new Response.Listener<AylaShare>() {
                    @Override
                    public void onResponse(AylaShare response) {
                        MainActivity.getInstance().dismissWaitDialog();
                        MainActivity.getInstance().getSupportFragmentManager().popBackStack();
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        MainActivity.getInstance().dismissWaitDialog();
                        Toast.makeText(MainActivity.getInstance(), error.getMessage(), Toast
                                .LENGTH_LONG).show();
                    }
                });

        MainActivity.getInstance().showWaitDialog(R.string.creating_share_title, R.string.creating_share_body);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId() ) {
            case R.id.action_unregister_device:
                if ( _deviceModel.getDevice().getGrant() == null ) {
                    unregisterDevice();
                } else {
                    removeShare();
                }
                break;

            case R.id.action_factory_reset_device:
                if ( _deviceModel.getDevice().getGrant() == null ) {
                    unregisterDevice();
                } else {
                    removeShare();
                }
                break;

            case R.id.action_timezone:
                updateTimezone();
                break;

            case R.id.action_device_details:
                showDetails();
                break;

            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @Override
    public void onAttach(Activity act) {
        super.onAttach(act);

        mContext = act;
    }

    @Override
    public void onAttach(Context cxt) {
        super.onAttach(cxt);

        mContext = cxt;
    }


    @Override
    public void onResume() {
        super.onResume();

        _deviceModel.getDevice().addListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        _deviceModel.getDevice().removeListener(this);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    void unregisterDeviceTimeout() {
        Logger.logInfo(LOG_TAG, "fr: unregister device [%s] timeout. ask again.", _deviceModel.getDevice().getDsn());
        Resources res = getActivity().getResources();
        new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.ic_launcher)
                .setTitle(res.getString(R.string.unregister_confirm_title))
                .setMessage(res.getString(R.string.unregister_failure_timeout))
                .setPositiveButton(R.string.unregister_try_again, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Put up a progress dialog
                        MainActivity.getInstance().showWaitDialog(getString(R.string.waiting_unregister_title),
                                getString(R.string.waiting_unregister_body));

                        Log.i(LOG_TAG, "Unregister Device: " + _deviceModel);
                        doUnregisterDevice(_deviceModel.getDevice());
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void unregisterDevice() {
        // Unregister Device clicked
        // Confirm first!
        Resources res = getActivity().getResources();
        new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.ic_launcher)
                .setTitle(res.getString(R.string.unregister_confirm_title))
                .setMessage(res.getString(R.string.unregister_confirm_body))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Put up a progress dialog
                        MainActivity.getInstance().showWaitDialog(getString(R.string.waiting_unregister_title),
                                getString(R.string.waiting_unregister_body));

                        Log.i(LOG_TAG, "Unregister Device: " + _deviceModel);
                        doUnregisterDevice(_deviceModel.getDevice());
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void doUnregisterDevice(AylaDevice device) {
        RequestFuture<AylaAPIRequest.EmptyResponse> future = RequestFuture.newFuture();
        final AylaAPIRequest request = device.unregister(future, future);
        try {
            AylaAPIRequest.EmptyResponse response = future.get(10 * 1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Toast.makeText(getActivity(), "Failed to unregister device", Toast.LENGTH_LONG).show();
            return;
        } catch (ExecutionException e) {
            Toast.makeText(getActivity(), "Failed to unregister device", Toast.LENGTH_LONG).show();
            return;
        } catch (TimeoutException e) {
            Toast.makeText(getActivity(), "Failed to unregister device", Toast.LENGTH_LONG).show();
            unregisterDeviceTimeout();
            return;
        }

        Log.i(LOG_TAG, "Device unregistered: " + device);
        Toast.makeText(getActivity(), R.string.unregister_success, Toast.LENGTH_SHORT).show();

        // Pop ourselves off of the back stack and force a refresh of the device list
        getFragmentManager().popBackStack();
        AMAPCore.sharedInstance().getDeviceManager().fetchDevices();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
    static class FactoryResetDeviceHandler extends Handler {
        private WeakReference<DeviceDetailFragment> _deviceDetailFragment;

        public FactoryResetDeviceHandler(DeviceDetailFragment deviceDetailFragment) {
            _deviceDetailFragment = new WeakReference<DeviceDetailFragment>(deviceDetailFragment);
        }

        @Override
        public void handleMessage(Message msg) {
            Logger.logMessage(LOG_TAG, msg, "fr: factory reset device");
            MainActivity.getInstance().dismissWaitDialog();
            if ( AylaNetworks.succeeded(msg) || (msg.arg1 == 404)) {
                Log.i(LOG_TAG, "fr: Device factory reset: " + _deviceDetailFragment.get()._deviceModel);

                Toast.makeText(_deviceDetailFragment.get().getActivity(), R.string.factory_reset_success, Toast.LENGTH_SHORT).show();

                // Pop ourselves off of the back stack and force a refresh of the device list
                _deviceDetailFragment.get().getFragmentManager().popBackStack();
                SessionManager.deviceManager().refreshDeviceList();
            } else {
                Log.e(LOG_TAG, "fr: Factory reset device failed for " + _deviceDetailFragment.get()._deviceModel + ": " + msg.obj);
                Toast.makeText(_deviceDetailFragment.get().getActivity(), R.string.factory_reset_failed, Toast.LENGTH_LONG).show();

                // if timeout, ask if they want to do it again?
                if (msg.arg1 == AylaNetworks.AML_ERROR_TIMEOUT) {
                    _deviceDetailFragment.get().factoryResetDeviceTimeout();
                }
            }
        }
    }

    void factoryResetDeviceTimeout() {
        Logger.logInfo(LOG_TAG, "fr: factory reset device [%s] timeout. ask again.", _deviceModel.getDevice().getDsn());
        Resources res = getActivity().getResources();
        new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.ic_launcher)
                .setTitle(res.getString(R.string.factory_reset_confirm_title))
                .setMessage(res.getString(R.string.factory_reset_failure_timeout))
                .setPositiveButton(R.string.factory_reset_try_again, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Put up a progress dialog
                        MainActivity.getInstance().showWaitDialog(getString(R.string.waiting_factory_reset_title),
                                getString(R.string.waiting_factory_reset_body));

                        Log.i(LOG_TAG, "Factory Reset Device: " + _deviceModel);
                        _deviceModel.factoryResetDevice(_factoryResetDeviceHandler);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private FactoryResetDeviceHandler _factoryResetDeviceHandler = new FactoryResetDeviceHandler(this);

    void factoryResetDevice() {
        Logger.logInfo(LOG_TAG, "fr: factory reset device [%s]", _deviceModel.getDevice().getDsn());
        Resources res = getActivity().getResources();
        new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.ic_launcher)
                .setTitle(res.getString(R.string.factory_reset_confirm_title))
                .setMessage(res.getString(R.string.factory_reset_confirm_body))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Put up a progress dialog
                        MainActivity.getInstance().showWaitDialog(getString(R.string.waiting_factory_reset_title),
                                getString(R.string.waiting_factory_reset_body));

                        Log.i(LOG_TAG, "Factory Reset Device: " + _deviceModel);
                        _deviceModel.factoryResetDevice(_factoryResetDeviceHandler);
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }
        */

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private void removeShare() {
        // Confirm first!
        Resources res = getActivity().getResources();

        new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.ic_launcher)
                .setTitle(res.getString(R.string.confirm_remove_share_title))
                .setMessage(res.getString(R.string.confirm_remove_shared_device_message_short))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.i(LOG_TAG, "Un-share Device: " + _deviceModel);
                        doRemoveShare();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void doRemoveShare() {
        AylaShare[] shares = fetchReceivedShares();
        if (shares == null) {
            return;
        }

        String dsn = _deviceModel.getDevice().getDsn();
        for (AylaShare share : shares) {
            if (share.getResourceId().equals(dsn)) {
                String shareId = share.getId();

                RequestFuture<AylaAPIRequest.EmptyResponse> future = RequestFuture.newFuture();
                final AylaAPIRequest request = AMAPCore.sharedInstance().getSessionManager().deleteShare(shareId, future, future);
                try {
                    AylaAPIRequest.EmptyResponse response = future.get(10 * 1000, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Toast.makeText(getActivity(), "Failed to delete share", Toast.LENGTH_LONG).show();
                    return;
                } catch (ExecutionException e) {
                    Toast.makeText(getActivity(), "Failed to delete share", Toast.LENGTH_LONG).show();
                    return;
                } catch (TimeoutException e) {
                    Toast.makeText(getActivity(), "Failed to delete share", Toast.LENGTH_LONG).show();
                    return;
                }

                Toast.makeText(MainActivity.getInstance(), R.string.share_removed, Toast.LENGTH_LONG).show();
                MainActivity.getInstance().getSupportFragmentManager().popBackStack();

                return;
            }
        }

        Toast.makeText(MainActivity.getInstance(), "This device was not shared with you", Toast.LENGTH_LONG).show();
    }

    private AylaShare[] fetchReceivedShares() {
        RequestFuture<AylaShare[]> future = RequestFuture.newFuture();
        final AylaAPIRequest request = AMAPCore.sharedInstance().getSessionManager().fetchReceivedShares(future, future);
        AylaShare[] shares = null;
        try {
            shares = future.get(10 * 1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Toast.makeText(getActivity(), "Failed to fetch shares", Toast.LENGTH_LONG).show();
            return null;
        } catch (ExecutionException e) {
            Toast.makeText(getActivity(), "Failed to fetch shares", Toast.LENGTH_LONG).show();
            return null;
        } catch (TimeoutException e) {
            Toast.makeText(getActivity(), "Failed to fetch shares", Toast.LENGTH_LONG).show();
            return null;
        }

        if (shares == null || shares.length == 0) {
            Toast.makeText(getActivity(), "No shares available", Toast.LENGTH_LONG).show();
            return null;
        }

        return shares;
    }

    private void notificationsClicked() {
        MainActivity.getInstance().pushFragment(NotificationListFragment.newInstance(_deviceModel));
    }

    private void scheduleClicked() {
        Fragment frag = _deviceModel.getScheduleFragment();
        MainActivity.getInstance().pushFragment(frag);
    }

    private void sharingClicked() {
        ShareDevicesFragment frag = ShareDevicesFragment.newInstance(this, _deviceModel.getDevice());
        MainActivity.getInstance().pushFragment(frag);
    }

    private void updateTimezone() {
        RequestFuture<AylaTimeZone> future = RequestFuture.newFuture();
        final AylaAPIRequest request = _deviceModel.getDevice().fetchTimeZone(future, future);
        AylaTimeZone tz = null;
        try {
            tz = future.get(10 * 1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Toast.makeText(getActivity(), "Failed to fetch current timezone", Toast.LENGTH_LONG).show();
            return;
        } catch (ExecutionException e) {
            Toast.makeText(getActivity(), "Failed to fetch current timezone", Toast.LENGTH_LONG).show();
            return;
        } catch (TimeoutException e) {
            Toast.makeText(getActivity(), "Failed to fetch current timezone", Toast.LENGTH_LONG).show();
            return;
        }

        if (tz == null) {
            Toast.makeText(getActivity(), "Failed to fetch current timezone", Toast.LENGTH_LONG).show();
            return;
        }

        chooseTimezone(tz);
    }

    private void showDetails() {
        Log.d(LOG_TAG, "showDetails");
        DeviceDetailListFragment frag = DeviceDetailListFragment.newInstance(_deviceModel.getDevice());
        MainActivity.getInstance().pushFragment(frag);
    }

    private void chooseTimezone(AylaTimeZone tz) {
        String currentTimezone = tz.tzId;
        final String[] timezones = TimeZone.getAvailableIDs();
        int checkedItem = -1;
        if ( currentTimezone != null ) {
            // Find the index of the item to check in our dialog's list
            for ( int i = 0; i < timezones.length; i++ ) {
                String tzStr = timezones[i];
                if (tzStr.equals(currentTimezone)) {
                    checkedItem = i;
                    break;
                }
            }
        }

        new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.ic_launcher)
                .setTitle(R.string.choose_timezone)
                .setSingleChoiceItems(timezones, checkedItem, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(LOG_TAG, "Item selected: " + timezones[which]);
                    }
                })
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ListView lv = ((AlertDialog)dialog).getListView();
                        int itemPos = lv.getCheckedItemPosition();
                        if ( itemPos > -1 ) {
                            Log.d(LOG_TAG, "Selected item: " + timezones[itemPos]);
                            setDeviceTimezone(timezones[itemPos]);
                        } else {
                            Log.d(LOG_TAG, "No selected item");
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .show();
    }

    @Override
    public void onClick(View v) {
        switch ( v.getId() ) {
            case R.id.notifications_button:
                notificationsClicked();
                break;

            case R.id.schedule_button:
                scheduleClicked();
                break;

            case R.id.sharing_button:
                sharingClicked();
                break;

            default:
                Log.e(LOG_TAG, "Unknown button click: " + v);
         }
    }

    private void setDeviceTimezone(String timezoneName) {
        MainActivity.getInstance().showWaitDialog(R.string.updating_timezone_title, R.string.updating_timezone_body);

        RequestFuture<AylaTimeZone> future = RequestFuture.newFuture();
        final AylaAPIRequest request = _deviceModel.getDevice().updateTimeZone(timezoneName, future, future);
        AylaTimeZone tz = null;
        try {
            tz = future.get(10 * 1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Toast.makeText(getActivity(), "Failed to update current timezone", Toast.LENGTH_LONG).show();
            return;
        } catch (ExecutionException e) {
            Toast.makeText(getActivity(), "Failed to update current timezone", Toast.LENGTH_LONG).show();
            return;
        } catch (TimeoutException e) {
            Toast.makeText(getActivity(), "Failed to update current timezone", Toast.LENGTH_LONG).show();
            return;
        }

        if (tz == null) {
            Toast.makeText(getActivity(), "Failed to update current timezone", Toast.LENGTH_LONG).show();
            return;
        }

        MainActivity.getInstance().dismissWaitDialog();
        Toast.makeText(MainActivity.getInstance(),
                R.string.timezone_update_success,
                Toast.LENGTH_LONG).show();
    }

    public class PropertyListAdapter extends ArrayAdapter<AylaProperty> {
        private final static String LOG_TAG = "PropertyListAdapter";
        private Context _context;

        public PropertyListAdapter(Context context, List<AylaProperty> objects) {
            super(context, 0, objects);
            _context = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final AylaProperty prop = getItem(position);
            if ( convertView == null ) {
                convertView = LayoutInflater.from(_context).inflate(R.layout.list_item_property, parent, false);
            }

            TextView propName = (TextView)convertView.findViewById(R.id.property_name);
            TextView propValueText = (TextView)convertView.findViewById(R.id.property_value_textview);
            final Switch propValueSwitch = (Switch)convertView.findViewById(R.id.property_value_switch);

            Log.d(LOG_TAG, "Property: " + prop.getName() + " Type: " + prop.getBaseType() + " Value: " + prop.getValue());

            propName.setText(_deviceModel.friendlyNameForPropertyName(prop.getName()));
            propValueText.setOnClickListener(null);
            if ( prop.getDirection().equals("output")) {
                // This is a read-only property
                propValueSwitch.setVisibility(View.GONE);
                propValueText.setVisibility(View.VISIBLE);
                propValueText.setText(prop.getValue().toString());
                propName.setTextColor(_context.getResources().getColor(R.color.disabled_text));
            } else {
                // This property can be set
                propName.setTextColor(_context.getResources().getColor(R.color.card_text));

                // Configure based on the base type of the property
                switch ( prop.getBaseType() ) {
                    case "boolean":
                        propValueSwitch.setVisibility(View.VISIBLE);
                        propValueSwitch.setEnabled(_deviceModel.isOnline());
                        propValueText.setVisibility(View.GONE);
                        propValueSwitch.setOnCheckedChangeListener(null);
                        propValueSwitch.setChecked("1".equals(prop.getValue()));
                        Log.d(LOG_TAG, "Checked: " + propValueSwitch.isChecked() + " prop.value: " + prop.getValue());
                        propValueSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            boolean _setting = false;

                            @Override
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                if (_setting) {
                                    return;
                                }

                                MainActivity.getInstance().showWaitDialog(R.string.please_wait, R.string.please_wait);
                                Boolean newValue = isChecked;
                                _deviceModel.setDatapoint(prop.getName(), newValue, new ViewModel.SetDatapointListener() {
                                    @Override
                                    public void setDatapointComplete(AylaDatapoint newDatapoint, AylaError error) {
                                        MainActivity.getInstance().dismissWaitDialog();
                                        Log.d(LOG_TAG, "setDatapoint error: " + error + " ***^^^");
                                    }
                                });


                            }
                        });
                        break;

                    case "string":
                    case "integer":
                    case "decimal":
                    default:
                        propValueSwitch.setVisibility(View.GONE);
                        propValueText.setVisibility(View.VISIBLE);
                        propValueText.setText(prop.getValue().toString());
                        propValueText.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                editProperty(prop);
                            }
                        });
                        break;
                }
            }

            return convertView;
        }
    }

    private void editProperty(AylaProperty property) {
        Log.d(LOG_TAG, "Edit Property: " +  property);
        Toast.makeText(getActivity(), "Edit " + property.getBaseType() + " property: Coming soon!", Toast.LENGTH_LONG).show();
    }
}
