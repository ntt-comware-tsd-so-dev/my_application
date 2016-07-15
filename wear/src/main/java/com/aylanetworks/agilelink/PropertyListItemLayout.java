package com.aylanetworks.agilelink;

import android.content.Context;
import android.support.wearable.view.WearableListView;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

public class PropertyListItemLayout extends RelativeLayout
        implements WearableListView.OnCenterProximityListener {

    private TextView mPropertyName;
    private Switch mReadWriteProperty;
    private RadioButton mReadOnlyProperty;
    private ImageView mRow;

    private boolean mFocused = false;

    public PropertyListItemLayout(Context context) {
        this(context, null);
    }

    public PropertyListItemLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PropertyListItemLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mPropertyName = (TextView) findViewById(R.id.property_name);
        mReadWriteProperty = (Switch) findViewById(R.id.rw_property);
        mReadOnlyProperty = (RadioButton) findViewById(R.id.ro_property);
        mRow = (ImageView) findViewById(R.id.row);

        final GestureDetector detector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent event) {
                if (mReadWriteProperty.getVisibility() != View.GONE) {
                    mReadWriteProperty.setChecked(!mReadWriteProperty.isChecked());
                }

                if (mRow.getVisibility() != View.GONE) {
                    mRow.performClick();
                }
                return true;
            }
        });

        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    detector.onTouchEvent(event);
                    return mFocused;
                } else {
                    return detector.onTouchEvent(event);
                }
            }
        });
    }

    @Override
    public void onCenterPosition(boolean animate) {
        mPropertyName.setAlpha(1);
        mReadOnlyProperty.setAlpha(1);
        mReadWriteProperty.setAlpha(1);
        mRow.setAlpha(1f);

        mFocused = true;
    }

    @Override
    public void onNonCenterPosition(boolean animate) {
        mPropertyName.setAlpha(0.7f);
        mReadOnlyProperty.setAlpha(0.7f);
        mReadWriteProperty.setAlpha(0.7f);
        mRow.setAlpha(0.5f);

        mFocused = false;
    }
}
