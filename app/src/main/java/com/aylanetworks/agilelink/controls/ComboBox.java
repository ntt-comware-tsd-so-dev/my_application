package com.aylanetworks.agilelink.controls;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListAdapter;

/*
 * ComboBox.java
 * AgileLink Application Framework
 *
 * Created by Brian King on 3/18/2015
 * Copyright (c) 2015 Ayla. All rights reserved.
 *
 * http://stackoverflow.com/questions/3024656/how-can-i-show-a-combobox-in-android
 */

public class ComboBox extends LinearLayout {
    private final static String LOG_TAG = "ComboBox";

    private AutoCompleteTextView _text;
    private ImageButton _button;

    public ComboBox(Context context) {
        super(context);
        this.createChildControls(context, null);
    }

    public ComboBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.createChildControls(context, attrs);
    }

    private final static String ANDROID_SCHEMA = "http://schemas.android.com/apk/res/android";

    private void createChildControls(Context context, AttributeSet attrs) {
        this.setOrientation(HORIZONTAL);
        this.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT));

        _text = new AutoCompleteTextView(context, attrs);
        _text.setSingleLine();

        this.addView(_text, new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT, 1));

        _button = new ImageButton(context);
        _button.setImageResource(android.R.drawable.arrow_down_float);
        _button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                _text.showDropDown();
            }
        });
        _button.setVisibility(View.GONE);
        this.addView(_button, new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT));
    }

    /**
     * Sets the source for DDLB suggestions.
     * Cursor MUST be managed by supplier!!
     *
     * @param adapter Source of suggestions.
     */
    public <T extends ListAdapter & Filterable> void setAdapter(T adapter) {
        if ( adapter != null && adapter.getCount() > 0 ) {
            _button.setVisibility(View.VISIBLE);
        } else {
            _button.setVisibility(View.GONE);
        }

        _text.setAdapter(adapter);
    }

    /**
     * Gets the text in the combo box.
     *
     * @return Text.
     */
    public String getText() {
        return _text.getText().toString();
    }

    /**
     * Sets the text in combo box.
     */
    public void setText(String text) {
        _text.setText(text);
    }
}