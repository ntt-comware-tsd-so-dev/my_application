package com.aylanetworks.agilelink.batch;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseExpandableListAdapter;

import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.actions.EditActionsFragment;
import com.aylanetworks.agilelink.fragments.GenericHelpFragment;
import com.aylanetworks.agilelink.framework.batch.BatchManager;
import com.aylanetworks.agilelink.framework.geofence.Action;
import com.aylanetworks.agilelink.framework.geofence.AylaDeviceActions;
import com.aylanetworks.agilelink.framework.batch.BatchAction;
import com.aylanetworks.aylasdk.AylaAPIRequest;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;
import com.aylanetworks.aylasdk.error.ServerError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import fi.iki.elonen.NanoHTTPD;

/*
 * AMAP_Android
 *
 * Copyright 2017 Ayla Networks, all rights reserved
 */

/**
 * BatchActionsFragment displays list of Device Actions for this User. The DeviceActions are
 * added from the Main Menu and clicking on Device Actions. The user needs to choose list of
 * actions for new Device Action or can update the existing Device Actions for the selected Batch
 * Action.
 */

public class BatchActionsFragment extends Fragment {
    private final static String LOG_TAG = "BatchActionsFragment";
    private ImageButton _addButton;
    private static final int MAX_BATCH_ACTIONS = 10;

    private ExpandableListView _expandableListView;
    private ArrayList<BatchAction> _batchActionList;
    private final HashMap<String, Action> _deviceActionMap = new HashMap<>();
    private BatchActionAdapter _adapter;


    public static BatchActionsFragment newInstance() {
        return new BatchActionsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater _inflater, ViewGroup container, Bundle savedInstanceState) {
        return _inflater.inflate(R.layout.fragment_all_batch_actions, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        _deviceActionMap.clear();

        _expandableListView = (ExpandableListView) view.findViewById(R.id.listViewBatches);
        _addButton = (ImageButton) view.findViewById(R.id.add_button);
        _addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addTapped();
            }
        });

        _expandableListView.setEmptyView(view.findViewById(R.id.batch_empty));
        _expandableListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (ExpandableListView.getPackedPositionType(id) == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
                    final BatchAction batchAction = _batchActionList.get(position);
                    if(batchAction != null) {
                        confirmRemoveBatchAction(batchAction);
                        return true;
                    }
                }
                return false;
            }
        });

        MainActivity.getInstance().showWaitDialog(null, null);
        BatchManager.fetchBatchActions(new Response.Listener<BatchAction[]>() {
            @Override
            public void onResponse(BatchAction[] batchActionsArray) {
                _batchActionList = new ArrayList<>(Arrays.asList
                        (batchActionsArray));
                AylaDeviceActions.fetchActions(new Response.Listener<Action[]>() {
                    @Override
                    public void onResponse(Action[] deviceActionsArray) {
                        ArrayList<Action> deviceActionList = new ArrayList<>(Arrays.asList(deviceActionsArray));
                        for (Action action : deviceActionList) {
                            _deviceActionMap.put(action.getId(), action);
                        }

                        MainActivity.getInstance().dismissWaitDialog();
                        doFillData(_batchActionList);
                    }
                }, new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        MainActivity.getInstance().dismissWaitDialog();
                        Log.d(LOG_TAG, error.getMessage());
                    }
                });
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                MainActivity.getInstance().dismissWaitDialog();
                //Check if there are no existing actions. This is not an actual error and we
                //don't want to show this error.
                ServerError serverError = ((ServerError) error);
                int code = serverError.getServerResponseCode();
                if (code == NanoHTTPD.Response.Status.NOT_FOUND.getRequestStatus()) {
                    Log.d(LOG_TAG, "No Existing Batch Actions");
                    return;
                }
                String errorString = MainActivity.getInstance().getString(R.string.Toast_Error) +
                        error.toString();
                Toast.makeText(MainActivity.getInstance(), errorString, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_help_automation, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.help_automation:
                showHelpFragment();
                return true;
        }
        return false;
    }
    private void showHelpFragment() {
        String fileURL = MainActivity.getInstance().getString(R.string.automation_help_url);
        MainActivity.getInstance().pushFragment(GenericHelpFragment.newInstance(fileURL));
    }

    private void addTapped() {
        Log.d(LOG_TAG, "Add button tapped");
        DeviceActionsFragment frag = DeviceActionsFragment.newInstance();
        MainActivity.getInstance().pushFragment(frag);
    }

    private void doFillData(ArrayList<BatchAction> batchActionsList) {
        showOrHideAddButton();
        _adapter = new BatchActionAdapter(batchActionsList);
        _adapter.setInflater((LayoutInflater) MainActivity.getInstance().getSystemService(Context.LAYOUT_INFLATER_SERVICE));
        _expandableListView.setAdapter(_adapter);
    }

    private void confirmRemoveBatchAction(final BatchAction batchAction) {
        new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.ic_launcher)
                .setMessage(MainActivity.getInstance().getString(R.string
                        .confirm_remove_action))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        BatchManager.deleteBatchAction(batchAction, new Response
                                .Listener<AylaAPIRequest
                                .EmptyResponse>() {
                            @Override
                            public void onResponse(AylaAPIRequest.EmptyResponse response) {
                                String msg = MainActivity.getInstance().getString(R.string.deleted_success);
                                Toast.makeText(MainActivity.getInstance(), msg, Toast.LENGTH_SHORT).show();
                                _adapter.notifyDataSetChanged();
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
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }


    /**
     * Hide Add Button if we reach MAX_BATCH_ACTIONS. Show it if it is less than MAX_BATCH_ACTIONS
     */
    private void showOrHideAddButton() {
        if (_batchActionList != null && _batchActionList.size() >= MAX_BATCH_ACTIONS) {
            _addButton.setVisibility(View.GONE);
        } else {
            _addButton.setVisibility(View.VISIBLE);
        }
    }

    public class BatchActionAdapter extends BaseExpandableListAdapter {
        private LayoutInflater _inflater;
        private final ArrayList<BatchAction> _batchActionList;

        public BatchActionAdapter(ArrayList<BatchAction> parents) {
            this._batchActionList = parents;
        }

        public void setInflater(LayoutInflater _inflater) {
            this._inflater = _inflater;
        }

        @Override
        public View getChildView(final int groupPosition, final int childPosition, boolean
                isLastChild, View convertView, ViewGroup parent) {
            final BatchAction batchAction = _batchActionList.get(groupPosition);
            final String[] actionUUIDs = batchAction.getActionUuids();
            final String deviceActionUUID = actionUUIDs[childPosition];
            TextView textBoxView;

            if (convertView == null) {
                convertView = _inflater.inflate(R.layout.device_action_row, parent,false);
            }

            textBoxView = (TextView) convertView.findViewById(R.id.textView1);
            Action action = _deviceActionMap.get(deviceActionUUID);
            if(action != null) {
                textBoxView.setText(action.getName());
            }

            final BatchActionAdapter actionAdapter = this;
            convertView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    confirmRemoveAction(batchAction, deviceActionUUID, actionAdapter, groupPosition);
                    return true;
                }
            });

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Action action = _deviceActionMap.get(deviceActionUUID);
                    EditActionsFragment frag = EditActionsFragment.newInstance(action.getDSN(), action);
                    MainActivity.getInstance().pushFragment(frag);
                }
            });


            return convertView;
        }

        private void confirmRemoveAction(final BatchAction batchAction,
                                         final String deviceActionUUID,
                                         final BatchActionAdapter actionAdapter,
                                         final int groupPosition) {
            new AlertDialog.Builder(getActivity())
                    .setIcon(R.drawable.ic_launcher)
                    .setMessage(MainActivity.getInstance().getString(R.string
                            .confirm_remove_action))
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //We remove this Device Action from the BatchAction and then Update
                            // that Batch Action
                            ArrayList<String> actionUUIDList = new ArrayList<>(Arrays.asList
                                    (batchAction.getActionUuids()));
                            if (actionUUIDList.contains(deviceActionUUID)) {
                                actionUUIDList.remove(deviceActionUUID);
                                String[] newArray = actionUUIDList.toArray(new String[actionUUIDList.size()]);
                                batchAction.setActionUuids(newArray);
                                updateBatchAction(batchAction, actionAdapter, groupPosition);
                            }
                        }
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .show();
        }

        private void updateBatchAction(final BatchAction action, final BatchActionAdapter
                actionAdapter, final int groupPosition) {
            BatchManager.updateBatchAction(action, new Response.Listener<AylaAPIRequest
                    .EmptyResponse>() {
                @Override
                public void onResponse(AylaAPIRequest.EmptyResponse response) {
                    String msg = MainActivity.getInstance().getString(R.string.deleted_success);
                    Toast.makeText(MainActivity.getInstance(), msg, Toast.LENGTH_SHORT).show();
                    _batchActionList.set(groupPosition, action);
                    actionAdapter.notifyDataSetChanged();
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

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = _inflater.inflate(R.layout.batch_action_group, parent, false);
            }

            ImageView iconExpand = (ImageView) convertView.findViewById(R.id.icon_expand);
            ImageView iconCollapse = (ImageView) convertView.findViewById(R.id.icon_collapse);

            if (isExpanded) {
                iconExpand.setVisibility(View.GONE);
                iconCollapse.setVisibility(View.VISIBLE);
            } else {
                iconExpand.setVisibility(View.VISIBLE);
                iconCollapse.setVisibility(View.GONE);
            }

            if (getChildrenCount(groupPosition) == 0) {
                iconExpand.setVisibility(View.GONE);
                iconCollapse.setVisibility(View.GONE);
            }
            TextView batchView = (TextView) convertView.findViewById(R.id.device_name_title);
            final BatchAction batchAction = _batchActionList.get(groupPosition);
            String batchActionName = batchAction.getName();
            batchView.setText(batchActionName);


            ImageView actionAddButton = (ImageView) convertView.findViewById(R.id.btn_add_action);
            actionAddButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DeviceActionsFragment frag = DeviceActionsFragment.newInstance(batchAction);
                    MainActivity.getInstance().pushFragment(frag);
                }
            });

            return convertView;
        }

        @Override
        public Object getChild(int groupPosition, int _actionsListPosition) {
            return null;
        }

        @Override
        public long getChildId(int groupPosition, int _actionsListPosition) {
            return 0;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return _batchActionList.get(groupPosition).getActionUuids().length;
        }

        @Override
        public Object getGroup(int groupPosition) {
            return null;
        }

        @Override
        public int getGroupCount() {
            return _batchActionList.size();
        }

        @Override
        public long getGroupId(int groupPosition) {
            return 0;
        }


        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int _actionsListPosition) {
            return false;
        }
    }
}
