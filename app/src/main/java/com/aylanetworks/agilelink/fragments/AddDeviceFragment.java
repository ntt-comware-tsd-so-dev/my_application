package com.aylanetworks.agilelink.fragments;

import android.os.Bundle;
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

import com.aylanetworks.agilelink.R;

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

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_device, container, false);

        // Get our description text view
        _descriptionTextView = (TextView)view.findViewById(R.id.registration_description);
        _descriptionTextView.setText(getActivity().getResources().getString(R.string.registration_same_lan_instructions));

        // Populate the spinners for product type & registration type
        Spinner s = (Spinner)view.findViewById(R.id.spinner_product_type);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(), R.array.product_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setSelection(PRODUCT_TYPE_EVB);
        s.setOnItemSelectedListener(this);
        s.setAdapter(adapter);

        s = (Spinner)view.findViewById(R.id.spinner_registration_type);
        adapter = ArrayAdapter.createFromResource(getActivity(), R.array.registration_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setSelection(REG_TYPE_SAME_LAN);
        s.setOnItemSelectedListener(this);
        s.setAdapter(adapter);

        // Hook up the "Register" button
        Button b = (Button)view.findViewById(R.id.register_button);
        b.setOnClickListener(this);

        return view;
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

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        Log.i(LOG_TAG, "Nothing Selected");
    }

    @Override
    public void onClick(View v) {
        // Register button clicked
        Log.i(LOG_TAG, "Register clicked");
    }
}
