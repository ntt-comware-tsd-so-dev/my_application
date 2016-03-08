package com.aylanetworks.agilelink.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.text.TextUtils;
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

import com.aylanetworks.aaml.AylaDatapoint;
import com.aylanetworks.aaml.AylaGrant;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaProperty;
import com.aylanetworks.aaml.AylaShare;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.device.DeviceUIProvider;
import com.aylanetworks.agilelink.device.GenericDevice;
import com.aylanetworks.agilelink.device.RemoteSwitchDevice;
import com.aylanetworks.agilelink.device.ZigbeeTriggerDevice;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.DeviceManager;
import com.aylanetworks.agilelink.framework.Gateway;
import com.aylanetworks.agilelink.framework.Logger;
import com.aylanetworks.agilelink.framework.SessionManager;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * DeviceDetailFragment.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 1/15/15.
 * Copyright (c) 2015 Ayla. All rights reserved.
 */

public class DeviceDetailFragment extends Fragment implements Device.DeviceStatusListener, View.OnClickListener, ShareDevicesFragment.ShareDevicesListener, Gateway.AylaGatewayCompletionHandler {

    public final static String LOG_TAG = "DeviceDetailFragment";

    public final static int FRAGMENT_RESOURCE_ID = R.layout.fragment_device_detail;

    public final static String ARG_DEVICE_DSN = "DeviceDSN";

    private Device _device;
    private ListView _listView;
    private PropertyListAdapter _adapter;
    private TextView _titleView;
    private TextView _dsnView;
    private ImageView _imageView;
    private Button _scheduleButton;
    private Button _notificationsButton;
    private Switch _identifySwitch;

    public static DeviceDetailFragment newInstance(Device device) {
        DeviceDetailFragment frag = new DeviceDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DEVICE_DSN, device.getDeviceDsn());
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        _device = null;
        if (getArguments() != null ) {
            String dsn = getArguments().getString(ARG_DEVICE_DSN);
            _device = SessionManager.deviceManager().deviceByDSN(dsn);
        }
     }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ensureIdentifyOff();
        if(SessionManager.deviceManager() != null){
            SessionManager.deviceManager().exitLANMode(new DeviceManager.LANModeListener(_device));
        }
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
        if(_device.isDeviceNode()){
            sharingButton.setVisibility(View.GONE);
        } else{
            sharingButton.setOnClickListener(this);
        }
        if ( !_device.getDevice().amOwner() ) {
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

        Button remoteButton = (Button)view.findViewById(R.id.remote_button);
        remoteButton.setOnClickListener(this);

        _identifySwitch = (Switch)view.findViewById(R.id.identify_button);
        _identifySwitch.setOnClickListener(this);

        Button triggerButton = (Button)view.findViewById(R.id.trigger_button);
        triggerButton.setOnClickListener(this);

        if (_device.isDeviceNode()) {
            Gateway gateway = Gateway.getGatewayForDeviceNode(_device);
            _identifySwitch.setVisibility(gateway.isZigbeeGateway() ? View.VISIBLE : View.GONE);

            if (_device instanceof ZigbeeTriggerDevice) {
                Logger.logDebug(LOG_TAG, "we are a trigger device.");
                triggerButton.setVisibility(View.VISIBLE);
            } else {
                triggerButton.setVisibility(View.GONE);
            }

            if (_device instanceof RemoteSwitchDevice) {
                Logger.logDebug(LOG_TAG, "rm: we are a remote.");
            } else {
                List<Device> remotes = gateway.getDevicesOfClass(new Class[]{RemoteSwitchDevice.class});
                if ((remotes != null) && (remotes.size() > 0)) {
                    Logger.logInfo(LOG_TAG, "rm: we have %d remotes.", remotes.size());
                    boolean pairable = false;
                    // Right now we have only one type of remote switch, but at some point there could be
                    // multiple types of remotes
                    for (Device device : remotes) {
                        RemoteSwitchDevice remote = (RemoteSwitchDevice) device;
                        if (remote.isPairableDevice(_device)) {
                            Logger.logInfo(LOG_TAG, "rm: device [%s:%s] is pairable with remote [%s:%s]",
                                    _device.getDeviceDsn(), _device.getClass().getSimpleName(),
                                    device.getDeviceDsn(), device.getClass().getSimpleName());
                            pairable = true;
                        }
                    }
                    if (!pairable) {
                        remoteButton.setVisibility(View.GONE);
                        Logger.logInfo(LOG_TAG, "rm: device [%s:%s] is not pairable with any of the available remotes.");
                    }
                } else {
                    remoteButton.setVisibility(View.GONE);
                    Logger.logInfo(LOG_TAG, "rm: we don't have any remotes.");
                }
            }
        } else {
            remoteButton.setVisibility(View.GONE);
            triggerButton.setVisibility(View.GONE);
            _identifySwitch.setVisibility(View.GONE);
        }

        updateUI();

        return view;
    }

    private void titleClicked() {
        // Let the user change the title
        final EditText editText = new EditText(getActivity());
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        editText.setText(_device.toString());
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

    private static class ChangeNameHandler extends Handler {
        private WeakReference<DeviceDetailFragment> _frag;
        private String _newDeviceName;

        public ChangeNameHandler(DeviceDetailFragment frag, String newDeviceName) {
            _frag = new WeakReference<DeviceDetailFragment>(frag);
            _newDeviceName = newDeviceName;
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity.getInstance().dismissWaitDialog();
            if ( AylaNetworks.succeeded(msg)) {
                _frag.get()._device.getDevice().productName = _newDeviceName;
                _frag.get().updateUI();

                // Let the world know something is different
                SessionManager.deviceManager().deviceChanged(_frag.get()._device);
            } else {
                Log.e(LOG_TAG, "Change name failed: " + msg);
                Toast.makeText(_frag.get().getActivity(), R.string.change_name_fail, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void changeDeviceName(String newDeviceName) {
        Map<String, String> params = new HashMap<>();
        params.put("productName", newDeviceName);
        _device.getDevice().update(new ChangeNameHandler(this, newDeviceName), params);
    }

    void updateUI() {
        if ( _device == null ) {
            Log.e(LOG_TAG, "Unable to find device!");
            getFragmentManager().popBackStack();
            Toast.makeText(getActivity(), R.string.unknown_error, Toast.LENGTH_SHORT).show();
        } else {
            // Get the property list and set up our adapter
            AylaProperty[] props = _device.getDevice().properties;
            if ( props != null ) {
                List<AylaProperty> propertyList = Arrays.asList(props);
                _adapter = new PropertyListAdapter(getActivity(), propertyList);
            } else {
                Log.e(LOG_TAG, "No properties found for device " + _device);
                _adapter = new PropertyListAdapter(getActivity(), new ArrayList<AylaProperty>());
            }

            // Can this device set schedules or property notifications?
            _scheduleButton.setVisibility(_device.getSchedulablePropertyNames().length > 0 ? View.VISIBLE : View.GONE);
            _notificationsButton.setVisibility(_device.getNotifiablePropertyNames().length > 0 ? View.VISIBLE : View.GONE);

            // Set the device title and image
            _titleView.setText(_device.toString());
            _dsnView.setText(_device.isInLanMode() ? _device.getDeviceIP() : _device.getDeviceDsn());
            _imageView.setImageDrawable(((DeviceUIProvider) _device).getDeviceDrawable(getActivity()));
            _listView.setAdapter(_adapter);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        MainActivity.getInstance().getMenuInflater().inflate(R.menu.menu_device_details, menu);

        boolean hasFactoryReset = false;
        if (_device != null) {
            // djunod: I have never been able to do Factory Reset on a Zigbee Gateway
            //hasFactoryReset = (_device.isGateway() || _device.isDeviceNode());
            hasFactoryReset = _device.isDeviceNode();
        }
        menu.getItem(1).setVisible(hasFactoryReset);
        menu.getItem(0).setVisible(!hasFactoryReset);

        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void shareDevices(String email, String role, Calendar startDate, Calendar endDate,
                             boolean readOnly, List<Device> devicesToShare) {

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        AylaShare share = new AylaShare();
        share.userEmail = email;
        if ( startDate != null ) {
            share.startDateAt = df.format(startDate.getTime());
        }
        if ( endDate != null ) {
            share.endDateAt = df.format(endDate.getTime());
        }
        if ( TextUtils.isEmpty(role) ) {
            share.operation = readOnly ? "read" : "write";
        } else {
            share.roleName = role;
        }


        MainActivity.getInstance().showWaitDialog(R.string.creating_share_title, R.string.creating_share_body);
        share.create(new CreateShareHandler(), _device.getDevice());
    }

    private static class DeleteShareHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Log.d(LOG_TAG, "Delete share: " + msg);
            if ( AylaNetworks.succeeded(msg) ) {
                Toast.makeText(MainActivity.getInstance(), R.string.share_removed, Toast.LENGTH_LONG).show();
                MainActivity.getInstance().getSupportFragmentManager().popBackStack();
            } else {
                String message = MainActivity.getInstance().getResources().getString(R.string.remove_share_failure);
                if ( msg.obj != null ) {
                    message = (String)msg.obj;
                }
                Toast.makeText(MainActivity.getInstance(), message, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId() ) {
            case R.id.action_unregister_device:
                if ( _device.getDevice().amOwner() ) {
                    unregisterDevice();
                } else {
                    removeShare();
                }
                break;

            case R.id.action_factory_reset_device:
                if ( _device.getDevice().amOwner() ) {
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
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Add ourselves as a listener for device updates
        SessionManager.deviceManager().addDeviceStatusListener(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if(SessionManager.deviceManager() != null){
            SessionManager.deviceManager().removeDeviceStatusListener(this);
        }
    }

    @Override
    public void statusUpdated(Device device, boolean changed) {
        Log.d(LOG_TAG, "statusUpdated: " + device);
        if ( changed && device.equals(_device) ) {
            updateUI();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    // Handler for device unregister call
    static class UnregisterDeviceHandler extends Handler {
        private WeakReference<DeviceDetailFragment> _deviceDetailFragment;

        public UnregisterDeviceHandler(DeviceDetailFragment deviceDetailFragment) {
            _deviceDetailFragment = new WeakReference<DeviceDetailFragment>(deviceDetailFragment);
        }

        @Override
        public void handleMessage(Message msg) {
            Logger.logMessage(LOG_TAG, msg, "fr: unregister device");
            MainActivity.getInstance().dismissWaitDialog();
            if ( AylaNetworks.succeeded(msg) ) {
                Log.i(LOG_TAG, "Device unregistered: " + _deviceDetailFragment.get()._device);

                Toast.makeText(_deviceDetailFragment.get().getActivity(), R.string.unregister_success, Toast.LENGTH_SHORT).show();

                // Pop ourselves off of the back stack and force a refresh of the device list
                _deviceDetailFragment.get().getFragmentManager().popBackStack();
                SessionManager.deviceManager().refreshDeviceList();
            } else {
                Log.e(LOG_TAG, "Unregister device failed for " + _deviceDetailFragment.get()._device + ": " + msg.obj);
                Toast.makeText(_deviceDetailFragment.get().getActivity(), R.string.unregister_failed, Toast.LENGTH_LONG).show();

                // if timeout, ask if they want to do it again?
                if (msg.arg1 == AylaNetworks.AML_ERROR_TIMEOUT) {
                    _deviceDetailFragment.get().unregisterDeviceTimeout();
                }
            }
        }
    }

    void unregisterDeviceTimeout() {
        Logger.logInfo(LOG_TAG, "fr: unregister device [%s] timeout. ask again.", _device.getDeviceDsn());
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

                        Log.i(LOG_TAG, "Unregister Device: " + _device);
                        _device.unregisterDevice(_unregisterDeviceHandler);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private UnregisterDeviceHandler _unregisterDeviceHandler = new UnregisterDeviceHandler(this);

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

                        Log.i(LOG_TAG, "Unregister Device: " + _device);
                        _device.unregisterDevice(_unregisterDeviceHandler);
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

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
                Log.i(LOG_TAG, "fr: Device factory reset: " + _deviceDetailFragment.get()._device);

                Toast.makeText(_deviceDetailFragment.get().getActivity(), R.string.factory_reset_success, Toast.LENGTH_SHORT).show();

                // Pop ourselves off of the back stack and force a refresh of the device list
                _deviceDetailFragment.get().getFragmentManager().popBackStack();
                SessionManager.deviceManager().refreshDeviceList();
            } else {
                Log.e(LOG_TAG, "fr: Factory reset device failed for " + _deviceDetailFragment.get()._device + ": " + msg.obj);
                Toast.makeText(_deviceDetailFragment.get().getActivity(), R.string.factory_reset_failed, Toast.LENGTH_LONG).show();

                // if timeout, ask if they want to do it again?
                if (msg.arg1 == AylaNetworks.AML_ERROR_TIMEOUT) {
                    _deviceDetailFragment.get().factoryResetDeviceTimeout();
                }
            }
        }
    }

    void factoryResetDeviceTimeout() {
        Logger.logInfo(LOG_TAG, "fr: factory reset device [%s] timeout. ask again.", _device.getDeviceDsn());
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

                        Log.i(LOG_TAG, "Factory Reset Device: " + _device);
                        _device.factoryResetDevice(_factoryResetDeviceHandler);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private FactoryResetDeviceHandler _factoryResetDeviceHandler = new FactoryResetDeviceHandler(this);

    void factoryResetDevice() {
        Logger.logInfo(LOG_TAG, "fr: factory reset device [%s]", _device.getDeviceDsn());
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

                        Log.i(LOG_TAG, "Factory Reset Device: " + _device);
                        _device.factoryResetDevice(_factoryResetDeviceHandler);
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

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
                        Log.i(LOG_TAG, "Un-share Device: " + _device);

                        AylaShare share = new AylaShare();
                        AylaGrant grant = _device.getDevice().grant;
                        share.id = grant.shareId;
                        share.delete(new DeleteShareHandler());
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void notificationsClicked() {
        MainActivity.getInstance().pushFragment(NotificationListFragment.newInstance(_device));
    }

    private void scheduleClicked() {
        Fragment frag = ((DeviceUIProvider)_device).getScheduleFragment();
        MainActivity.getInstance().pushFragment(frag);
    }

    private void remoteClicked() {
        Fragment frag = ((DeviceUIProvider)_device).getRemoteFragment();
        MainActivity.getInstance().pushFragment(frag);
    }

    private void triggerClicked() {
        Fragment frag = ((DeviceUIProvider)_device).getTriggerFragment();
        MainActivity.getInstance().pushFragment(frag);
    }

    public void gatewayCompletion(Gateway gateway, Message msg, Object tag) {
        Switch control = (Switch)tag;
        control.setEnabled(true);
        Logger.logInfo(LOG_TAG, "adn: identify [%s] %s - done", _device.getDeviceDsn(), (control.isChecked() ? "ON" : "OFF"));
    }

    private void identifyClicked(View v) {
        Switch control = (Switch)v;
        control.setEnabled(false);
        Gateway gateway = Gateway.getGatewayForDeviceNode(_device);
        Logger.logInfo(LOG_TAG, "adn: identify [%s] %s - start", _device.getDeviceDsn(), (control.isChecked() ? "ON" : "OFF"));
        gateway.identifyDeviceNode(_device, control.isChecked(), 255, v, this);
    }

    private void ensureIdentifyOff() {
        if ((_device != null) && _device.isDeviceNode()) {
            Gateway gateway = Gateway.getGatewayForDeviceNode(_device);
            // we don't care about the results
            Logger.logInfo(LOG_TAG, "adn: identify [%s] OFF - start", _device.getDeviceDsn());
            gateway.identifyDeviceNode(_device, false, 0, null, null);
        }
    }

    private void sharingClicked() {
        ShareDevicesFragment frag = ShareDevicesFragment.newInstance(this, (GenericDevice)_device);
        MainActivity.getInstance().pushFragment(frag);
    }

    private static class CreateShareHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Log.d(LOG_TAG, "CreateShareHandler: " + msg);
            MainActivity.getInstance().dismissWaitDialog();
            if ( AylaNetworks.succeeded(msg) ) {
                Toast.makeText(MainActivity.getInstance(), R.string.share_device_success, Toast.LENGTH_SHORT).show();
                MainActivity.getInstance().getSupportFragmentManager().popBackStack();
            } else {
                String error = (String)msg.obj;
                if (TextUtils.isEmpty(error)) {
                    error = MainActivity.getInstance().getString(R.string.share_device_fail);
                }
                Toast.makeText(MainActivity.getInstance(), error, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void updateTimezone() {
        // Fetch the timezone for the device
        MainActivity.getInstance().showWaitDialog(R.string.fetching_timezone_title, R.string.fetching_timezone_body);
        _device.fetchTimezone(new Device.DeviceStatusListener() {
            @Override
            public void statusUpdated(Device device, boolean changed) {
                MainActivity.getInstance().dismissWaitDialog();
                if (changed) {
                    chooseTimezone();
                } else {
                    Toast.makeText(MainActivity.getInstance(),
                            R.string.timezone_fetch_failed,
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void showDetails() {
        Log.d(LOG_TAG, "showDetails");
        DeviceDetailListFragment frag = DeviceDetailListFragment.newInstance(_device);
        MainActivity.getInstance().pushFragment(frag);
    }

    private void chooseTimezone() {
        String currentTimezone = _device.getDevice().timezone.tzId;
        final String[] timezones = TimeZone.getAvailableIDs();
        int checkedItem = -1;
        if ( currentTimezone != null ) {
            // Find the index of the item to check in our dialog's list
            for ( int i = 0; i < timezones.length; i++ ) {
                String tz = timezones[i];
                if (tz.equals(currentTimezone)) {
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

            case R.id.remote_button:
                remoteClicked();
                break;

            case R.id.trigger_button:
                triggerClicked();
                break;

            case R.id.identify_button:
                identifyClicked(v);
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
        _device.setTimeZone(timezoneName, new Device.DeviceStatusListener() {
            @Override
            public void statusUpdated(Device device, boolean changed) {
                MainActivity.getInstance().dismissWaitDialog();
                Toast.makeText(MainActivity.getInstance(),
                        changed ? R.string.timezone_update_success : R.string.timezone_update_failure,
                        Toast.LENGTH_LONG).show();
            }
        });
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

            Log.d(LOG_TAG, "Property: " + prop.name() + " Type: " + prop.baseType + " Value: " + prop.value);

            propName.setText(_device.friendlyNameForPropertyName(prop.name()));
            propValueText.setOnClickListener(null);
            if ( prop.direction().equals("output")) {
                // This is a read-only property
                propValueSwitch.setVisibility(View.GONE);
                propValueText.setVisibility(View.VISIBLE);
                propValueText.setText(prop.value);
                propName.setTextColor(_context.getResources().getColor(R.color.disabled_text));
            } else {
                // This property can be set
                propName.setTextColor(_context.getResources().getColor(R.color.card_text));

                // Configure based on the base type of the property
                switch ( prop.baseType ) {
                    case "boolean":
                        propValueSwitch.setVisibility(View.VISIBLE);
                        propValueSwitch.setEnabled(_device.isOnline());
                        propValueText.setVisibility(View.GONE);
                        propValueSwitch.setOnCheckedChangeListener(null);
                        propValueSwitch.setChecked("1".equals(prop.value));
                        Log.d(LOG_TAG, "Checked: " + propValueSwitch.isChecked() + " prop.value: " + prop.value);
                        propValueSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            boolean _setting = false;

                            @Override
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                if (_setting) {
                                    return;
                                }

                                MainActivity.getInstance().showWaitDialog(R.string.please_wait, R.string.please_wait);
                                Boolean newValue = isChecked;
                                _device.setDatapoint(prop.name(), newValue, new Device.SetDatapointListener() {
                                    @Override
                                    public void setDatapointComplete(boolean succeeded, AylaDatapoint newDatapoint) {
                                        MainActivity.getInstance().dismissWaitDialog();
                                        if (succeeded && newDatapoint != null) {
                                            _setting = true;
                                            propValueSwitch.setChecked("1".equals(newDatapoint.value()));
                                            _setting = false;
                                        } else {
                                            Log.e(LOG_TAG, "Set property failed");
                                        }
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
                        propValueText.setText(prop.value);
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
        Toast.makeText(getActivity(), "Edit " + property.baseType + " property: Coming soon!", Toast.LENGTH_LONG).show();
    }
}
