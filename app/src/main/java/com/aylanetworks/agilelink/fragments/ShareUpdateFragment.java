package com.aylanetworks.agilelink.fragments;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.android.volley.Response;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.framework.AMAPCore;
import com.aylanetworks.aylasdk.AylaLog;
import com.aylanetworks.aylasdk.AylaShare;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import static com.aylanetworks.aylasdk.AylaShare.ShareAccessLevel;

/**
 * AMAP_Android
 * <p>
 * Copyright 2016 Ayla Networks, all rights reserved
 */
public class ShareUpdateFragment extends android.support.v4.app.Fragment {

    private final static String LOG_TAG = "SHARE_UPDATE";
    private AylaShare _share;
    private EditText _edittextDsn;
    private EditText _edittextEmail;
    private EditText _edittextRole;
    private RadioGroup _radioGrp;
    private Button _startButton;
    private Button _endButton;
    private Calendar _shareStartDate;
    private Calendar _shareEndDate;
    private DateFormat _dateFormat;
    private SimpleDateFormat _simpleDateFormat;
    private Button _shareUpdateButton;

    public static ShareUpdateFragment newInstance(AylaShare share) {
        Bundle args = new Bundle();
        args.putSerializable(MainActivity.ARG_SHARE, share);
        ShareUpdateFragment fragment = new ShareUpdateFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _simpleDateFormat= new SimpleDateFormat
                ("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        _simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        _dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_share_update, container, false);
        _share = (AylaShare) getArguments().getSerializable(MainActivity.ARG_SHARE);
        _edittextDsn= (EditText) root.findViewById(R.id.share_dsn);
        _edittextDsn.setText(_share.getResourceId());
        _edittextEmail= (EditText) root.findViewById(R.id.share_email);
        _edittextEmail.setText(_share.getUserEmail());
        _edittextRole= (EditText) root.findViewById(R.id.share_role);
        _edittextRole.setText(_share.getRoleName());
        _radioGrp = (RadioGroup) root.findViewById(R.id.access_radio_group);
        _startButton = (Button)root.findViewById(R.id.button_start_date);
        _endButton = (Button)root.findViewById(R.id.button_end_date);
        _shareUpdateButton = (Button)root.findViewById(R.id.share_update_button);

        String accessLevel = _share.getOperation();
        if(accessLevel != null){
            _radioGrp.check(accessLevel.equals(ShareAccessLevel.READ.stringValue())?
                    R.id.radio_view: R.id.radio_control);
        }

        String startDate = _share.getStartDateAt();
        if(startDate != null){
            try {
                _shareStartDate = Calendar.getInstance();
                _shareStartDate.setTime(_simpleDateFormat.parse(startDate));
                _startButton.setText(_dateFormat.format(_shareStartDate.getTime()));

            } catch (ParseException e) {
                AylaLog.e(LOG_TAG, "Exception while parsing start date: "+startDate +
                        " "+e.getMessage());
                _startButton.setText(getString(R.string.no_date));
                _shareStartDate = null;
            }
        } else{
            _startButton.setText(getString(R.string.now));
        }
        String endDate = _share.getEndDateAt();
        if( endDate != null){
            try {
                _shareEndDate = Calendar.getInstance();
                _shareEndDate.setTime(_simpleDateFormat.parse(endDate));
                _endButton.setText(_dateFormat.format(_shareEndDate.getTime()));

            } catch (ParseException e) {
                AylaLog.e(LOG_TAG, "Exception while parsing end date: "+endDate +
                        " "+e.getMessage());
                _endButton.setText(getString(R.string.no_date));
                _shareEndDate = null;
            }
        } else{
            _endButton.setText(getString(R.string.never));
        }

        _startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseDate((Button)v);
            }
        });
        _endButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseDate((Button)v);
            }
        });

        _shareUpdateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String operation = _radioGrp.getCheckedRadioButtonId() == R.id.radio_view?
                        ShareAccessLevel.READ.stringValue():
                        ShareAccessLevel.WRITE.stringValue();


                String startDate = null;
                if(_shareStartDate != null){
                    startDate = _simpleDateFormat.format(_shareStartDate.getTime());
                }
                String endDate = null;
                if(_shareEndDate != null){
                    endDate = _simpleDateFormat.format(_shareEndDate.getTime());
                }
                _share.setRoleName(_edittextRole.getText().toString());
                _share.setOperation(operation);
                _share.setStartDateAt(startDate);
                _share.setEndDateAt(endDate);
                MainActivity.getInstance().showWaitDialog(getString(R.string.share_updating),
                        null );
                AMAPCore.sharedInstance().getSessionManager().updateShare(_share, null,
                        new Response.Listener<AylaShare>() {
                    @Override
                    public void onResponse(AylaShare response) {
                        Toast.makeText(getContext(), getString(R.string.share_update_success),
                                Toast.LENGTH_SHORT).show();
                        MainActivity.getInstance().dismissWaitDialog();
                        MainActivity.getInstance().getSupportFragmentManager().popBackStack();

                    }
                }, new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        MainActivity.getInstance().dismissWaitDialog();
                        Toast.makeText(getContext(), String.format("%s. %s",
                                getString(R.string.share_update_error), error.getMessage()),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        return root;

    }

    private void chooseDate(final Button button) {
        Calendar now = Calendar.getInstance();
        final int id = button.getId();

        DatePickerDialog d = new DatePickerDialog(getActivity(),
                new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                if(id == R.id.button_start_date){
                    if(_shareStartDate == null){
                        _shareStartDate = Calendar.getInstance();
                    }
                    _shareStartDate.set(Calendar.YEAR, year);
                    _shareStartDate.set(Calendar.MONTH, monthOfYear);
                    _shareStartDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                } else{
                    if(_shareEndDate == null){
                        _shareEndDate = Calendar.getInstance();
                    }
                    _shareEndDate.set(Calendar.YEAR, year);
                    _shareEndDate.set(Calendar.MONTH, monthOfYear);
                    _shareEndDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                }
                updateButtonText();

            }
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
        d.setButton(DialogInterface.BUTTON_NEUTRAL, getResources().getString(R.string.no_date),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if(id == R.id.button_start_date){
                            _shareStartDate  = null;
                        } else{
                            _shareEndDate = null;
                        }
                        updateButtonText();
                    }
                });
        d.getDatePicker().setMinDate(now.getTimeInMillis());
        d.show();
    }

    private void updateButtonText() {
        if ( _shareStartDate == null) {
            _startButton.setText(R.string.now);
        } else {
            _startButton.setText(_dateFormat.format(_shareStartDate.getTime()));
        }

        if ( _shareEndDate == null) {
            _endButton.setText(R.string.never);
        } else {
            _endButton.setText(_dateFormat.format(_shareEndDate.getTime()));
        }
    }

}
