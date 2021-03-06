package com.aylanetworks.agilelink.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Paint;
import android.os.Bundle;
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
import com.aylanetworks.agilelink.ErrorUtils;
import com.aylanetworks.agilelink.framework.AMAPCore;
import com.aylanetworks.agilelink.framework.ViewModel;
import com.aylanetworks.agilelink.framework.automation.Automation;
import com.aylanetworks.agilelink.framework.automation.AutomationManager;
import com.aylanetworks.agilelink.framework.geofence.Action;
import com.aylanetworks.agilelink.framework.geofence.AylaDeviceActions;
import com.aylanetworks.agilelink.framework.batch.BatchAction;
import com.aylanetworks.agilelink.framework.batch.BatchManager;
import com.aylanetworks.aylasdk.AylaAPIRequest;
import com.aylanetworks.aylasdk.AylaDatapoint;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaDeviceManager;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
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
        AMAPCore sharedInstance = AMAPCore.sharedInstance();
        if (getArguments() != null && sharedInstance != null && sharedInstance.getDeviceManager() != null) {
            String dsn = getArguments().getString(ARG_DEVICE_DSN);
            AylaDevice d = sharedInstance.getDeviceManager().deviceWithDSN(dsn);
            _deviceModel = sharedInstance.getSessionParameters().viewModelProvider
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
        editText.setText(_deviceModel.getDevice().getFriendlyName());
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
        Toast.makeText(getActivity(),
                ErrorUtils.getUserMessage(getActivity(), error, R.string.unknown_error),
                Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(MainActivity.getInstance(),
                                ErrorUtils.getUserMessage(getContext(), error, R.string.change_name_fail),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    void updateUI() {
        if(!isAdded()){
            return;
        }
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
        _titleView.setText(_deviceModel.getDevice().getFriendlyName());
        _dsnView.setText(_deviceModel.isInLanMode() ? _deviceModel.getDevice().getLanIp() :
                _deviceModel.getDevice().getDsn());
        _imageView.setImageDrawable(_deviceModel.getDeviceDrawable(getActivity()));
        _listView.setAdapter(_adapter);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        MainActivity.getInstance().getMenuInflater().inflate(R.menu.menu_device_details, menu);

        boolean hasFactoryReset = false;
        if (_deviceModel != null) {
            hasFactoryReset = _deviceModel.getDevice().isNode();
        }
        menu.getItem(1).setVisible(hasFactoryReset);
        menu.getItem(0).setVisible(!hasFactoryReset);

        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void shareDevices(String email, String role, Calendar startDate, Calendar endDate,
                             boolean readOnly, List<AylaDevice> devicesToShare) {

        final SimpleDateFormat dateFormat = new SimpleDateFormat
                ("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);

        String startDateString = null;
        String endDateString = null;

        if ( startDate != null ) {
            startDateString = dateFormat.format(startDate.getTime());
        }
        if ( endDate != null ) {
            endDateString = dateFormat.format(endDate.getTime());
        }

        if (role.equals("")) {
            role = null;
        }

        AylaShare share = _deviceModel.getDevice().shareWithEmail(email,
                readOnly ? AylaShare.ShareAccessLevel.READ.stringValue() :
                        AylaShare.ShareAccessLevel.WRITE.stringValue(), role, startDateString,
                endDateString);

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
                        Toast.makeText(MainActivity.getInstance(),
                                ErrorUtils.getUserMessage(getContext(), error, R.string.share_device_fail),
                                Toast.LENGTH_LONG).show();
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
                fetchTimeZones();
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

    private void doUnregisterDevice(final AylaDevice device) {
        device.unregister(
                new Response.Listener<AylaAPIRequest.EmptyResponse>() {
                    @Override
                    public void onResponse(AylaAPIRequest.EmptyResponse response) {
                        Log.i(LOG_TAG, "Device unregistered: " + device);
                        Toast.makeText(getActivity(), R.string.unregister_success, Toast.LENGTH_SHORT).show();
                        deleteActionsForDevice(device);
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        MainActivity.getInstance().dismissWaitDialog();
                        Toast.makeText(MainActivity.getInstance(),
                                ErrorUtils.getUserMessage(getContext(), error, R.string.unregister_failed),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Delete all the actions for this Device. As this device is unregistered in the above method
     * need to make sure actions(if any) for this device saved in user datum are cleaned up.
     * @param device device whose actions need to be deleted
     */
    private void deleteActionsForDevice(final AylaDevice device) {
        AylaDeviceActions.fetchActions(new Response.Listener<Action[]>() {
            final HashSet<String> actionIDSet = new HashSet<>();

            @Override
            public void onResponse(Action[] arrayAction) {
                for (final Action action : arrayAction) {
                    if (device.getDsn().equalsIgnoreCase(action.getDSN())) {
                        actionIDSet.add(action.getId());
                    }
                }
                if (!actionIDSet.isEmpty()) {
                    deleteActionList(actionIDSet);
                }
                refreshDevices();
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                Log.e(LOG_TAG, "fetchActions: " + error);
                refreshDevices();
            }
        });
    }

    /**
     * Delete all actions from User datum for the set of Action Id's.
     * @param actionIDSet set of actionId's to delete from user datum
     */
    private void deleteActionList(final HashSet<String> actionIDSet) {
        AylaDeviceActions.deleteActions(actionIDSet, new Response.Listener<HashSet<String>>() {
                    @Override
                    public void onResponse(HashSet<String> deletedSet) {
                        fetchBatchActions(deletedSet);
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        Log.e(LOG_TAG, "deleteAction: " + error);
                    }
                });
    }

    /**
     * Fetch all BatchActions and check if any of the BatchAction has device actions that are
     * removed. If the BatchAction consists entirely of a list of Device Actions that were
     * deleted we can safely delete that BatchAction else just update that BatchAction by
     * removing the list of Device Actions that are passed.
     * @param deviceActionSet Set of Device Action ids that were removed
     */
    private void fetchBatchActions(final HashSet<String> deviceActionSet) {
        BatchManager.fetchBatchActions(new Response.Listener<BatchAction[]>() {
            @Override
            public void onResponse(BatchAction[] response) {
                ArrayList<String> groupActionDeleteList = new ArrayList<>();
                ArrayList<BatchAction> updateActionList = new ArrayList<>();
                //If all the Device Actions in a BatchAction are deleted then delete that
                // BatchAction else Update the BatchAction
                for(BatchAction groupAction: response) {
                    ArrayList<String> actionUUIDList =new ArrayList<>(Arrays.asList(groupAction
                            .getActionUuids()));
                    if(deviceActionSet.containsAll(actionUUIDList)) {
                        groupActionDeleteList.add(groupAction.getUuid());
                    } else {
                        actionUUIDList.removeAll(deviceActionSet);
                        groupAction.setActionUuids(actionUUIDList.toArray(new String[actionUUIDList.size()]));
                        updateActionList.add(groupAction);
                    }
                }
                if(updateActionList.isEmpty()) {
                    deleteBatchActions(groupActionDeleteList);
                } else {
                    updateAndDeleteBatchActions(groupActionDeleteList,updateActionList);
                }
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                Log.e(LOG_TAG, "fetchBatchActions: " + error);
            }
        });
    }

    /**
     * First update all the BatchActions passed in updateActionList and then on success/failure
     * delete the BatchActions passed in deleteActionList
     * @param deleteActionList UUIDs of BatchActions that need to be deleted
     * @param updateActionList List of BatchActions that need to be Updated
     */
    private void updateAndDeleteBatchActions(final ArrayList<String> deleteActionList,
                                             final ArrayList<BatchAction> updateActionList) {
        BatchManager.updateBatchActions(updateActionList, new Response.Listener<ArrayList<BatchAction>>
                () {
            @Override
            public void onResponse(ArrayList<BatchAction>response) {
                deleteBatchActions(deleteActionList);
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                deleteBatchActions(deleteActionList);
                String errorString = MainActivity.getInstance().getString(R.string.Toast_Error) +
                        error.toString();
                Toast.makeText(MainActivity.getInstance(), errorString, Toast.LENGTH_LONG).show();
            }
        });
    }


    private void deleteBatchActions(final ArrayList<String> deleteActionList) {
        BatchManager.deleteBatchActions(deleteActionList, new Response.Listener<ArrayList<String>>() {
            @Override
            public void onResponse(ArrayList<String>response) {
                Log.i(LOG_TAG, "deleteBatchActions success");
                removeAutomatedActions(response);
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                String errorString = MainActivity.getInstance().getString(R.string.Toast_Error) +
                        error.toString();
                Toast.makeText(MainActivity.getInstance(), errorString, Toast.LENGTH_LONG).show();

            }
        });
    }


    /**
     * Check if the deleted Batch Actions are in automation and remove these Batch Actions from the
     * automation
     *
     * @param actionIDsToRemove actionId set that are removed
     */
    private void removeAutomatedActions(final ArrayList<String> actionIDsToRemove) {
        final ArrayList<Automation> automationList = new ArrayList<>();
        AutomationManager.fetchAutomation(new Response.Listener<Automation[]>() {
            @Override
            public void onResponse(Automation[] response) {
                for (Automation automation : response) {
                    String[] actionsArray = automation.getActions();
                    if (actionsArray != null) {
                        List<String> list = new ArrayList<>(Arrays.asList(actionsArray));
                        //Remove all the actionID collection from this automation list
                        if (list.removeAll(actionIDsToRemove)) {
                            String[] updatedActionList = list.toArray(new String[list.size()]);
                            automation.setActions(updatedActionList);
                            automationList.add(automation);
                        }
                    }
                }
                if (!automationList.isEmpty()) {
                    updateAutomations(automationList);
                }
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                Log.e(LOG_TAG, error.getMessage());
            }
        });
    }

    /**
     * Updated Automations from User datum for the list of automations.
     * @param automationList automation list that need to be updated
     */
    private void updateAutomations(final ArrayList<Automation> automationList) {
        AutomationManager.updateAutomations(automationList, new Response.Listener<ArrayList<Automation>>() {
            @Override
            public void onResponse(ArrayList<Automation> automationArrayList) {
                Log.i(LOG_TAG, "updateAutomations success");
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                Log.e(LOG_TAG, "updateAutomations: " + error.getMessage());
            }
        });
    }

    private void refreshDevices() {
        // Pop ourselves off of the back stack and force a refresh of the device list
        AMAPCore.sharedInstance().getDeviceManager().fetchDevices();
        MainActivity.getInstance().dismissWaitDialog();
        getFragmentManager().popBackStack();
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////

    /*
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
                        fetchReceivedSharesAndRemoveCurrent();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void removeCurrentShare(AylaShare[] shares) {
        String dsn = _deviceModel.getDevice().getDsn();
        for (AylaShare share : shares) {
            if (share.getResourceId().equals(dsn)) {
                String shareId = share.getId();

                AMAPCore.sharedInstance().getSessionManager().deleteShare(shareId,
                        new Response.Listener<AylaAPIRequest.EmptyResponse>() {
                            @Override
                            public void onResponse(AylaAPIRequest.EmptyResponse response) {
                                Toast.makeText(MainActivity.getInstance(), R.string.share_removed, Toast.LENGTH_LONG).show();
                                MainActivity.getInstance().getSupportFragmentManager().popBackStack();
                            }
                        },
                        new ErrorListener() {
                            @Override
                            public void onErrorResponse(AylaError error) {
                                Toast.makeText(MainActivity.getInstance(),
                                        ErrorUtils.getUserMessage(getContext(), error, R.string.remove_share_failure),
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                return;
            }
        }
    }

    private void fetchReceivedSharesAndRemoveCurrent() {
        AMAPCore.sharedInstance().getSessionManager().fetchReceivedShares(
                new Response.Listener<AylaShare[]>() {
                    @Override
                    public void onResponse(AylaShare[] response) {
                        removeCurrentShare(response);
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        Toast.makeText(MainActivity.getInstance(),
                                ErrorUtils.getUserMessage(getContext(), error, R.string.timezone_fetch_failed),
                                Toast.LENGTH_LONG).show();
                    }
                });
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

    private void fetchTimeZones() {
        _deviceModel.getDevice().fetchTimeZone(
                new Response.Listener<AylaTimeZone>() {
                    @Override
                    public void onResponse(AylaTimeZone response) {
                        chooseTimezone(response);
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        Toast.makeText(MainActivity.getInstance(),
                                ErrorUtils.getUserMessage(getContext(), error, R.string.timezone_fetch_failed),
                                Toast.LENGTH_LONG).show();
                    }
                });
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

        _deviceModel.getDevice().updateTimeZone(timezoneName,
                new Response.Listener<AylaTimeZone>() {
                    @Override
                    public void onResponse(AylaTimeZone response) {
                        MainActivity.getInstance().dismissWaitDialog();
                        Toast.makeText(MainActivity.getInstance(),
                                R.string.timezone_update_success,
                                Toast.LENGTH_LONG).show();
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        Toast.makeText(MainActivity.getInstance(),
                                ErrorUtils.getUserMessage(getContext(), error, R.string.timezone_update_failure),
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

            propName.setText(_deviceModel.friendlyNameForPropertyName(prop.getName()).trim());
            propValueText.setOnClickListener(null);

            if (prop.getDirection().equals("output")) {
                // This is a read-only property
                propValueSwitch.setVisibility(View.GONE);
                propValueText.setVisibility(View.VISIBLE);

                if (prop.getValue() != null) {
                    propValueText.setText(prop.getValue().toString().trim());
                } else {
                    propValueText.setText("N/A");
                }

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

                        if (prop.getValue() != null ) {
                            propValueSwitch.setChecked((int) prop.getValue() == 1);
                        } else {
                            propValueSwitch.setChecked(false);
                        }

                        Log.d(LOG_TAG, "Checked: " + propValueSwitch.isChecked() + " prop.value: " + prop.getValue());
                        propValueSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            boolean _setting = false;

                            @Override
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                if (_setting) {
                                    return;
                                }

                                _deviceModel.setDatapoint(prop.getName(), isChecked ? 1 : 0, new ViewModel.SetDatapointListener() {
                                    @Override
                                    public void setDatapointComplete(AylaDatapoint newDatapoint, AylaError error) {
                                        if(AMAPCore.sharedInstance() != null){
                                            AylaDeviceManager dm = AMAPCore.sharedInstance()
                                                    .getDeviceManager();
                                            if( dm!=null && dm.getState() !=
                                                    AylaDeviceManager.DeviceManagerState.Paused){
                                                updateUI();
                                            }
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

                        if (prop.getValue() != null) {
                            propValueText.setText(prop.getValue().toString().trim());
                        } else {
                            propValueText.setText("N/A");
                        }

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