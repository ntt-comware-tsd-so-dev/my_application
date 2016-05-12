package com.aylanetworks.agilelink.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.aylanetworks.aylasdk.AylaNetworks;
import com.aylanetworks.aylasdk.AylaReachability;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.SessionManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Brian King on 4/7/15.
 */
public class AboutFragment extends Fragment {
    protected ListView _listView;
    protected TextView _headerTextView;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_about, container, false);

        _headerTextView = (TextView)v.findViewById(R.id.page_header);
        _headerTextView.setText(getString(R.string.about_app, getString(R.string.app_name)));

        _listView = (ListView)v.findViewById(R.id.list_view);

        populateList();

        return v;
    }

    protected void populateList() {
        List<AboutItem> items = new ArrayList<>();
        SessionManager.SessionParameters params = SessionManager.sessionParameters();

        items.add(new AboutItem(getString(R.string.service_type), SessionManager.getServiceTypeString()));
        items.add(new AboutItem(getString(R.string.app_version), params.appVersion));
        items.add(new AboutItem(getString(R.string.library_version), AylaNetworks.getAamlVersion()));

        String connectivity;
        switch (AylaReachability.getConnectivity()) {
            case AylaNetworks.AML_REACHABILITY_REACHABLE:
                connectivity = getString(R.string.reachable);
                break;

            case AylaNetworks.AML_REACHABILITY_UNREACHABLE:
                connectivity = getString(R.string.unreachable);
                break;

            case AylaNetworks.AML_REACHABILITY_UNKNOWN:
            default:
                connectivity = getString(R.string.unknown);
        }
        items.add(new AboutItem(getString(R.string.service_reachability), connectivity));

        _listView.setAdapter(new AboutListAdapter(getActivity(), items.toArray(new AboutItem[items.size()])));
    }

    protected class AboutItem {
        public AboutItem(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String name;
        public String value;
    }

    protected class AboutListAdapter extends ArrayAdapter<AboutItem> {
        public AboutListAdapter(Context context, AboutItem[] objects) {
            super(context, R.layout.about_list_item, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if ( convertView == null ) {
                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.about_list_item, parent, false);
            }

            AboutItem item = getItem(position);
            TextView tv = (TextView)convertView.findViewById(R.id.name_text);
            tv.setText(item.name);

            tv = (TextView)convertView.findViewById(R.id.value_text);
            tv.setText(item.value);

            return convertView;
        }
    }
}
