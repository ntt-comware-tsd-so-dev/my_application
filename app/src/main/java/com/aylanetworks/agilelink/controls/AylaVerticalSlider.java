package com.aylanetworks.agilelink.controls;
/* 
 * AylaVerticalSlider
 * AgileLink Application Framework
 *
 * Created by David N. Junod on 6/25/15.
 * Copyright (c) 2015 Ayla Networks. All rights reserved.
 */

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.SeekBar;

public class AylaVerticalSlider extends SeekBar {

    private static final String LOG_TAG = "AylaVerticalSlider";

    private static final int SCROLL_THRESHOLD = 40;

    private OnSeekBarChangeListener onChangeListener;
    private int lastProgress = 0;

    public AylaVerticalSlider(Context context) {
        super(context);
    }

    public AylaVerticalSlider(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public AylaVerticalSlider(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(h, w, oldh, oldw);
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(heightMeasureSpec, widthMeasureSpec);
        setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
    }

    protected void onDraw(Canvas c) {
        c.rotate(-90);
        c.translate(-getHeight(), 0);

        super.onDraw(c);
    }

    @Override
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener onChangeListener) {
        this.onChangeListener = onChangeListener;
    }

    private float mFirstY;
    private boolean mScrolling;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }

        float diffY = event.getY() - mFirstY;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                onChangeListener.onStartTrackingTouch(this);
                setPressed(true);
                setSelected(true);
                mFirstY = event.getY();
                mScrolling = false;
                break;

            case MotionEvent.ACTION_MOVE:
                super.onTouchEvent(event);

                // see if they are actually trying to scroll
                if (mScrolling || (Math.abs(diffY) > SCROLL_THRESHOLD)) {
                    mScrolling = true;

                    // Compute progress
                    int progress = getMax() - (int) (getMax() * event.getY() / getHeight());

                    // Ensure progress stays within boundaries
                    if (progress < 0) {
                        progress = 0;
                    }
                    if (progress > getMax()) {
                        progress = getMax();
                    }
                    // Draw progress
                    setProgress(progress);

                    if (progress != lastProgress) {
                        // Only enact listener if the progress has actually changed
                        lastProgress = progress;
                        onChangeListener.onProgressChanged(this, progress, true);
                    }
                } else {
                    // Draw progress
                    setProgress(lastProgress);
                }
                onSizeChanged(getWidth(), getHeight(), 0, 0);
                setPressed(true);
                setSelected(true);
                break;

            case MotionEvent.ACTION_UP:
                onChangeListener.onStopTrackingTouch(this);
                setPressed(false);
                setSelected(false);
                break;

            case MotionEvent.ACTION_CANCEL:
                super.onTouchEvent(event);
                setPressed(false);
                setSelected(false);
                break;
        }
        return true;
    }

    public synchronized void setProgressAndThumb(int progress) {
        setProgress(progress);
        onSizeChanged(getWidth(), getHeight(), 0, 0);
        if (progress != lastProgress) {
            // Only enact listener if the progress has actually changed
            lastProgress = progress;
            onChangeListener.onProgressChanged(this, progress, true);
        }
    }

    public synchronized void setMaximum(int maximum) {
        setMax(maximum);
    }

    public synchronized int getMaximum() {
        return getMax();
    }
}
