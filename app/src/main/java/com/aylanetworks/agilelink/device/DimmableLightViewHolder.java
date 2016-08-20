package com.aylanetworks.agilelink.device;

import android.content.res.Resources;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.aylanetworks.agilelink.framework.ViewModel;
import com.aylanetworks.aylasdk.AylaDatapoint;
import com.aylanetworks.aylasdk.AylaProperty;
import com.aylanetworks.agilelink.MainActivity;
import com.aylanetworks.agilelink.R;
import com.aylanetworks.agilelink.controls.AylaVerticalSlider;
import com.aylanetworks.aylasdk.error.AylaError;

/*
 * DimmableLightViewHolder.java
 * AgileLink Application Framework
 *
 * Created by David N. Junod on 6/25/15.
 * Copyright (c) 2015 Ayla Networks. All rights reserved.
 */

public class DimmableLightViewHolder extends GenericDeviceViewHolder implements SeekBar.OnSeekBarChangeListener {

    private final static String LOG_TAG = "DimmableLightViewHolder";
    public final static String DIMMABLE_PROP_NAME = "dimmer";

    public View _switchButton;
    public AylaVerticalSlider _slider;
    public TextView _switchLabel;
    public View _switchActivityView;

    public DimmableLightViewHolder(View view) {
        super(view);
        _switchButton = view.findViewById(R.id.toggle_switch_container);
        _switchLabel = (TextView) view.findViewById(R.id.control_circle);
        _slider = (AylaVerticalSlider)view.findViewById(R.id.control_slider);
        _switchActivityView = view.findViewById(R.id.control_activity_container);
    }

    public String getDimmablePropertyName() {
        return DIMMABLE_PROP_NAME;
    }

    void updateDimmableProperty(int value) {
        String propertyName = getDimmablePropertyName();
        AylaProperty prop = _currentDeviceModel.getProperty(propertyName);
        if (prop == null) {
            Resources res = MainActivity.getInstance().getResources();
            Toast.makeText(MainActivity.getInstance(), res.getString(R.string.no_property), Toast.LENGTH_SHORT).show();
            return;
        }

        showSwitchBusyIndicator(true);
        _currentDeviceModel.setDatapoint(propertyName, value, new ViewModel.SetDatapointListener() {
            @Override
            public void setDatapointComplete(AylaDatapoint newDatapoint, AylaError error) {
                showSwitchBusyIndicator(false);
            }
        });
    }

    void updateSwitch() {
        String propertyName = _currentDeviceModel.getObservablePropertyName();
        AylaProperty prop = _currentDeviceModel.getProperty(propertyName);
        if (prop == null) {
            Resources res = MainActivity.getInstance().getResources();
            Toast.makeText(MainActivity.getInstance(), res.getString(R.string.no_property), Toast.LENGTH_SHORT).show();
            return;
        }

        // Get the opposite boolean value and set it
        showSwitchBusyIndicator(true);
        final Boolean newValue = "0".equals(prop.getValue());
        _currentDeviceModel.setDatapoint(propertyName, newValue, new ViewModel.SetDatapointListener() {
            @Override
            public void setDatapointComplete(AylaDatapoint newDatapoint, AylaError error) {
                Resources res = MainActivity.getInstance().getResources();
                _switchLabel.setText(res.getString(newValue ? R.string.switched_off_name : R.string.switched_on_name));
                showSwitchBusyIndicator(false);
            }
        });
    }

    void showSwitchBusyIndicator(boolean value) {
        showSwitchBusyIndicator(value, true);
    }

    void showSwitchBusyIndicator(boolean value, boolean anim) {
        long duration = (anim) ? 500 : 100;

        if (value) {
            AlphaAnimation fadeInAnimation = new AlphaAnimation(0.0f, 1.0f);
            fadeInAnimation.setDuration(duration);
            fadeInAnimation.setFillAfter(true);
            _switchActivityView.startAnimation(fadeInAnimation);
            _switchActivityView.setVisibility(View.VISIBLE);

        } else {
            AlphaAnimation fadeOutAnimation = new AlphaAnimation(1.0f, 0.0f);
            fadeOutAnimation.setDuration(duration);
            fadeOutAnimation.setFillAfter(true);
            fadeOutAnimation.setAnimationListener(new Animation.AnimationListener() {

                @Override
                public void onAnimationEnd(Animation animation) {
                    _switchActivityView.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }

                @Override
                public void onAnimationStart(Animation animation) {
                }
            });
            _switchActivityView.startAnimation(fadeOutAnimation);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Dimmer control

    int _sliderStartProgress = 0;
    int _sliderViewValue;

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        // so the display tracks the change
        _sliderViewValue = progress;
        //mUsageView.setText(AylaClientDevice.getDimmableLevelAsString(progress));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        _sliderStartProgress = seekBar.getProgress();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // see if they are just trying to turn the light on/off
        // this is one way of doing it, another would be to track if the touch moved
        if (_sliderStartProgress != seekBar.getProgress()) {
            _sliderViewValue = seekBar.getProgress();
            updateDimmableProperty(_sliderViewValue);
            /*
            showSwitchBusyIndicator(true);
            if (!_currentDeviceModel.updateDimmableProperty(_sliderViewValue, this, _currentDeviceModel)) {
                Toast.makeText(getActivity(), getString(R.string.no_property), Toast.LENGTH_SHORT).show();
                showSwitchBusyIndicator(h, false);
            }
            */
        } else {
            // Toggle the button state
            updateSwitch();
        }
    }

    ///////////////////////////////////////////////////////////////////////////

}
