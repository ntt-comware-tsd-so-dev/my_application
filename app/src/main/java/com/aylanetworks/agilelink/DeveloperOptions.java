package com.aylanetworks.agilelink;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.Spinner;

import com.aylanetworks.aylasdk.AylaCache;
import com.aylanetworks.aylasdk.AylaNetworks;
import com.aylanetworks.aylasdk.AylaSystemSettings;

/**
 * Created by Raji Pillay on 9/21/15.
 */

//// TODO: 6/3/16 BSK: This class has not yet been refactored for 5.0 SDK, needs some thought

public class DeveloperOptions extends Activity {
    private static String LOCATION_US = "US";
    private static String LOCATION_CHINA = "CN";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.developer_options);
        final Spinner locationSpinner = (Spinner) findViewById(R.id.location_spinner);
        final Spinner serviceSpinner = (Spinner) findViewById(R.id.service_spinner);
        final ListView configSpinner = (ListView) findViewById(R.id.config_listview);
        final Button saveButton = (Button) findViewById(R.id.save_button);

        final ArrayAdapter<CharSequence> locationAdapter = ArrayAdapter.createFromResource(this, R
                .array.location_options, R.layout.spinner_item);
        final ArrayAdapter<CharSequence> serviceAdapter = ArrayAdapter.createFromResource(this, R
                .array.service_options, R.layout.spinner_item);
        final ArrayAdapter<CharSequence> configAdapter = ArrayAdapter.createFromResource(this, R
                .array.config_options, R.layout.config_list_item);

        locationSpinner.setAdapter(locationAdapter);
        serviceSpinner.setAdapter(serviceAdapter);
        configSpinner.setAdapter(configAdapter);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int location = locationSpinner.getSelectedItemPosition();
                int service = serviceSpinner.getSelectedItemPosition();

                switch (location) {
                    case 0: //US
                        setServiceLocationWithCountryCode(LOCATION_US);
                        break;
                    case 1:
                        setServiceLocationWithCountryCode(LOCATION_CHINA);
                        break;
                    default:
                        setServiceLocationWithCountryCode(LOCATION_US);

                }

                switch (service) {
                    case 0:
                        setServiceType(AylaSystemSettings.ServiceType.Dynamic);
                        break;
                    case 1:
                        setServiceType(AylaSystemSettings.ServiceType.Development);
                        break;
                    case 2:
                        setServiceType(AylaSystemSettings.ServiceType.Field);
                        break;
                    case 3:
                        setServiceType(AylaSystemSettings.ServiceType.Staging);
                        break;
                    default:
                        setServiceType(AylaSystemSettings.ServiceType.Dynamic);

                }

                finish();
            }
        });

        configSpinner.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
               View v = parent.getChildAt(position);
                CheckedTextView txtview = (CheckedTextView) v.findViewById(R.id.config_textview);
                switch (position) {
                    case 0:
                        if(txtview.isChecked()){
//                            AylaSystemUtils.loggingEnabled = AylaNetworks.YES;
//                            AylaSystemUtils.loggingLevel = AylaNetworks.AML_LOGGING_LEVEL_INFO;
//                            AylaSystemUtils.loggingInit();
                        }

                        break;
                    case 1:
                        if(txtview.isChecked()){
//                            AylaSystemUtils.clearAllCaches = AylaNetworks.YES;
//                            AylaCache.clearAll();
//                            AylaSystemUtils.saveSetting("currentUser", ""); // clear user auth info
//                            AylaSystemUtils.saveSetting("currentPageNo", 0); // clear last page displayed
                        }

                        break;
                    case 2:
                        if(txtview.isChecked()){
//                            AylaSystemUtils.newDeviceToServiceConnectionRetries = 16;
//                            AylaSystemUtils.slowConnection = AylaNetworks.YES;
                        }
                        break;
                    case 3:
                        if(txtview.isChecked()){
//                            AylaSystemUtils.lanModeState = AylaNetworks.lanMode.ENABLED;
                        }
                        break;
                }
            }
        });


    }

    private void setServiceLocationWithCountryCode(String locationUs) {

    }

    private void setServiceType(AylaSystemSettings.ServiceType serviceType) {

    }
}