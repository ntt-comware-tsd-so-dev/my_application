package com.aylanetworks.agilelink.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.aylanetworks.aaml.AylaDevice;
import com.aylanetworks.aaml.AylaNetworks;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.fragments.adapters.DeviceListAdapter;
import com.aylanetworks.agilelink.fragments.adapters.DeviceTypeAdapter;
import com.aylanetworks.agilelink.framework.Device;
import com.aylanetworks.agilelink.framework.DeviceManager;
import com.aylanetworks.agilelink.framework.SessionManager;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Brian King on 1/21/15.
 */
public class AddDeviceFragment extends Fragment implements AdapterView.OnItemSelectedListener, View.OnClickListener {
    private static final String LOG_TAG = "AddDeviceFragment";

    private static final int PRODUCT_TYPE_EVB = 0;
    private static final int PRODUCT_TYPE_SMART_PLUG = 1;
    private static final int PRODUCT_TYPE_OTHER = 2;

    private static final int REG_TYPE_SAME_LAN = 0;
    private static final int REG_TYPE_BUTTON_PUSH = 1;
    private static final int REG_TYPE_DISPLAY = 2;

    public static AddDeviceFragment newInstance() {
        AddDeviceFragment frag = new AddDeviceFragment();
        return frag;
    }

    private TextView _descriptionTextView;
    private List<Device> _deviceList;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_device, container, false);

        // Get our description text view
        _descriptionTextView = (TextView)view.findViewById(R.id.registration_description);
        _descriptionTextView.setText(getActivity().getResources().getString(R.string.registration_same_lan_instructions));

        // Populate the spinners for product type & registration type
        Spinner s = (Spinner)view.findViewById(R.id.spinner_product_type);
        s.setOnItemSelectedListener(this);
        s.setAdapter(createProductTypeAdapter());

        s = (Spinner)view.findViewById(R.id.spinner_registration_type);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(), R.array.registration_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setSelection(REG_TYPE_SAME_LAN);
        s.setOnItemSelectedListener(this);
        s.setAdapter(adapter);

        // Hook up the "Register" button
        Button b = (Button)view.findViewById(R.id.register_button);
        b.setOnClickListener(this);

        return view;
    }

    ArrayAdapter<Device> createProductTypeAdapter() {
        List<Class<? extends Device>> deviceClasses = SessionManager.sessionParameters().deviceCreator.getSupportedDeviceClasses();
        ArrayList<Device> deviceList = new ArrayList<>();
        for ( Class<? extends Device> c : deviceClasses ) {
            try {
                AylaDevice fakeDevice = new AylaDevice();
                Device d = c.getDeclaredConstructor(AylaDevice.class).newInstance(fakeDevice);
                deviceList.add(d);
            } catch (java.lang.InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        ArrayAdapter<Device> adapter = new DeviceTypeAdapter(getActivity(),
                deviceList.toArray(new Device[deviceList.size()]));

        return adapter;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if ( parent.getId() == R.id.spinner_product_type ) {
            // Update the product type. We will set the appropriate value on the registration
            // type when this is selected.
            Spinner regTypeSpinner = (Spinner)getView().findViewById(R.id.spinner_registration_type);

            switch ( position ) {
                case PRODUCT_TYPE_EVB:
                    regTypeSpinner.setSelection(REG_TYPE_SAME_LAN);
                    break;

                case PRODUCT_TYPE_SMART_PLUG:
                    regTypeSpinner.setSelection(REG_TYPE_BUTTON_PUSH);
                    break;

                case PRODUCT_TYPE_OTHER:
                default:
                        // Do nothing
                        ;
            }
        } else if ( parent.getId() == R.id.spinner_registration_type ) {
            // Update the display text
            int textId;
            switch ( position ) {
                case REG_TYPE_BUTTON_PUSH:
                default:
                    textId = R.string.registration_button_push_instructions;
                    break;

                case REG_TYPE_SAME_LAN:
                    textId = R.string.registration_same_lan_instructions;
                    break;

                case REG_TYPE_DISPLAY:
                    textId = R.string.registration_display_instructions;
                    break;
            }
            _descriptionTextView.setText(getActivity().getResources().getString(textId));
        }

        Log.i(LOG_TAG, "Selected " + position);
    }

    private String getSelectedRegistrationType() {
        Spinner regTypeSpinner = (Spinner)getView().findViewById(R.id.spinner_registration_type);
        switch ( regTypeSpinner.getSelectedItemPosition() ) {
            case REG_TYPE_SAME_LAN:
                return AylaNetworks.AML_REGISTRATION_TYPE_SAME_LAN;
            case REG_TYPE_BUTTON_PUSH:
                return AylaNetworks.AML_REGISTRATION_TYPE_BUTTON_PUSH;
            case REG_TYPE_DISPLAY:
                return AylaNetworks.AML_REGISTRATION_TYPE_DISPLAY;
        }
        return null;
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        Log.i(LOG_TAG, "Nothing Selected");
    }

    private Handler _registerHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.i(LOG_TAG, "Register handler called: " + msg);
            MainActivity.getInstance().dismissWaitDialog();
            if ( msg.arg1 >= 200 && msg.arg1 < 300 ) {
                // Success!
                Toast.makeText(getActivity(), R.string.registration_success, Toast.LENGTH_LONG).show();
                getActivity().getSupportFragmentManager().popBackStack();
                SessionManager.deviceManager().refreshDeviceList();
                SessionManager.deviceManager().refreshDeviceStatus(null);
            } else {
                // Something went wrong
                Toast.makeText(getActivity(), R.string.registration_failure, Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    public void onClick(View v) {
        // Register button clicked
        Log.i(LOG_TAG, "Register clicked");

        MainActivity.getInstance().showWaitDialog(null, null);
        AylaDevice newDevice = new AylaDevice();
        newDevice.registrationType = getSelectedRegistrationType();
        newDevice.registerNewDevice(_registerHandler);
    }
}
