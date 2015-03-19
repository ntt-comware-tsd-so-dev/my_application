package com.aylanetworks.agilelink.fragments;

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

import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.fragments.adapters.ContactListAdapter;
import com.aylanetworks.agilelink.framework.ContactManager;
import com.aylanetworks.agilelink.framework.SessionManager;

/**
 * Created by Brian King on 3/19/15.
 */
public class ContactListFragment extends Fragment implements View.OnClickListener {
    private final static String LOG_TAG = "ContactListFrag";

    private RecyclerView _recyclerView;
    private RecyclerView.LayoutManager _layoutManager;

    public static ContactListFragment newInstance() {
        return new ContactListFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_aldevice, container, false);

        _recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        _recyclerView.setHasFixedSize(true);

        _layoutManager = new LinearLayoutManager(getActivity());
        _recyclerView.setLayoutManager(_layoutManager);

        ImageButton b = (ImageButton) view.findViewById(R.id.add_button);
        b.setOnClickListener(this);

        // Do a deep fetch of the contact list
        MainActivity.getInstance().showWaitDialog(R.string.fetching_contacts_title, R.string.fetching_contacts_body);
        ContactManager cm = SessionManager.getInstance().getContactManager();
        cm.fetchContacts(new ContactManager.ContactManagerListener() {
            @Override
            public void contactListUpdated(ContactManager manager, boolean succeeded) {
                MainActivity.getInstance().dismissWaitDialog();
                _recyclerView.setAdapter(new ContactListAdapter());
            }
        }, true);

        return view;

    }

    @Override
    public void onClick(View v) {
        Log.d(LOG_TAG, "Add button clicked");
    }
}
