package com.aylanetworks.agilelink.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.aaml.AylaProperty;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.SessionManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Brian King on 1/15/15.
 */
public class DeviceDetailFragment extends Fragment implements Device.DeviceStatusListener, View.OnClickListener {
    public final static String LOG_TAG = "DeviceDetailFragment";

    public final static String ARG_DEVICE_DSN = "DeviceDSN";

    private Device _device;
    private ListView _listView;
    private PropertyListAdapter _adapter;
    private TextView _titleView;
    private ImageView _imageView;

    public static DeviceDetailFragment newInstance(Device device) {
        DeviceDetailFragment frag = new DeviceDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DEVICE_DSN, device.getDevice().dsn);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        _device = null;
        if (getArguments() != null ) {
            String dsn = getArguments().getString(ARG_DEVICE_DSN);
            _device = SessionManager.deviceManager().deviceByDSN(dsn);
        }
     }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_device_detail, container, false);

        _listView = (ListView)view.findViewById(R.id.listView);
        _titleView = (TextView)view.findViewById(R.id.device_name);
        _imageView = (ImageView)view.findViewById(R.id.device_image);

        Button button = (Button)view.findViewById(R.id.unregister_device);
        button.setOnClickListener(this);

        button = (Button)view.findViewById(R.id.notifications_button);
        button.setOnClickListener(this);

        button = (Button)view.findViewById(R.id.schedule_button);
        button.setOnClickListener(this);

        updateUI();

        return view;
    }

    void updateUI() {
        if ( _device == null ) {
            Log.e(LOG_TAG, "Unable to find device!");
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

            // Set the device title and image
            _titleView.setText(_device.toString());
            _imageView.setImageDrawable(_device.getDeviceDrawable(getActivity()));
            _listView.setAdapter(_adapter);
        }
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

        SessionManager.deviceManager().removeDeviceStatusListener(this);
    }

    @Override
    public void statusUpdated(Device device) {
        if ( device.equals(_device) ) {
            updateUI();
        }
    }

    // Handler for device unregister call
    static class UnregisterDeviceHandler extends Handler {
        private WeakReference<DeviceDetailFragment> _deviceDetailFragment;
        public UnregisterDeviceHandler(DeviceDetailFragment deviceDetailFragment) {
            _deviceDetailFragment = new WeakReference<DeviceDetailFragment>(deviceDetailFragment);
        }

        @Override
        public void handleMessage(Message msg) {
            if ( msg.what == AylaNetworks.AML_ERROR_OK ) {
                Log.i(LOG_TAG, "Device unregistered: " + _deviceDetailFragment.get()._device);

                Toast.makeText(_deviceDetailFragment.get().getActivity(), R.string.unregister_success, Toast.LENGTH_SHORT).show();

                // Pop ourselves off of the back stack and force a refresh of the device list
                _deviceDetailFragment.get().getFragmentManager().popBackStack();
                SessionManager.deviceManager().refreshDeviceList();
            } else {
                Log.e(LOG_TAG, "Unregister device failed for " + _deviceDetailFragment.get()._device + ": " + msg.obj);
                Toast.makeText(_deviceDetailFragment.get().getActivity(), R.string.unregister_failed, Toast.LENGTH_LONG).show();
            }
        }
    }

    private UnregisterDeviceHandler _unregisterDeviceHandler = new UnregisterDeviceHandler(this);

    private void unregisterDevice() {
        // Unregister Device clicked
        // Confirm first!
        Resources res = getActivity().getResources();
        new AlertDialog.Builder(getActivity())
                .setTitle(res.getString(R.string.unregister_confirm_title))
                .setMessage(res.getString(R.string.unregister_confirm_body))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.i(LOG_TAG, "Unregister Device: " + _device);
                        _device.getDevice().unregisterDevice(_unregisterDeviceHandler);
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .setIcon(R.drawable.ic_launcher)
                .show();
    }

    private void notificationsClicked() {
        Toast.makeText(getActivity(), "Notifications: Coming soon!", Toast.LENGTH_SHORT).show();
    }

    private void scheduleClicked() {
        Toast.makeText(getActivity(), "Schedule: Coming soon!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(View v) {
        switch ( v.getId() ) {
            case R.id.unregister_device:
                unregisterDevice();
                break;

            case R.id.notifications_button:
                notificationsClicked();
                break;

            case R.id.schedule_button:
                scheduleClicked();
                break;

            default:
                Log.e(LOG_TAG, "Unknown button click: " + v);
         }
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
            AylaProperty prop = getItem(position);
            if ( convertView == null ) {
                convertView = LayoutInflater.from(_context).inflate(R.layout.cardview_generic_device, parent, false);
            }

            TextView propName = (TextView)convertView.findViewById(R.id.device_name);
            TextView propValue = (TextView)convertView.findViewById(R.id.device_state);

            propName.setText(prop.name());
            propValue.setText(prop.value);

            return convertView;
        }
    }
}
