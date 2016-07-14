package com.aylanetworks.agilelink;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.support.wearable.view.WearableListView;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class PropertyListView extends WearableListView {

    private static final int OVERSCROLL_ADJUST_ROW_PX = 60;
    public enum ScrollStatus {SCROLL_IDLE, SCROLL_NOT_IDLE,
        SHOW_NEXT_ROW_HINT, HIDE_NEXT_ROW_HINT,
        SHOW_PREVIOUS_ROW_HINT, HIDE_PREVIOUS_ROW_HINT,
        ADJUST_NEXT_ROW, ADJUST_PREVIOUS_ROW}

    private ScrollStatusListener mStatusListener;

    private int mScrollingCentralPosition = 0;
    private int mSettledCentralPosition = 0;
    private float mInitialTouchY;

    public PropertyListView(Context context) {
        this(context, null);
    }

    public PropertyListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PropertyListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            mInitialTouchY = event.getY();
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (mSettledCentralPosition == getAdapter().getItemCount() - 1) {
                if (mInitialTouchY - event.getY() > OVERSCROLL_ADJUST_ROW_PX) {
                    mStatusListener.onScrollStatusChanged(ScrollStatus.SHOW_NEXT_ROW_HINT);
                } else {
                    mStatusListener.onScrollStatusChanged(ScrollStatus.HIDE_NEXT_ROW_HINT);
                }
            } else if (mSettledCentralPosition == 0) {
                if (event.getY() - mInitialTouchY > OVERSCROLL_ADJUST_ROW_PX) {
                    mStatusListener.onScrollStatusChanged(ScrollStatus.SHOW_PREVIOUS_ROW_HINT);
                } else {
                    mStatusListener.onScrollStatusChanged(ScrollStatus.HIDE_PREVIOUS_ROW_HINT);
                }
            }
        } else if (action == MotionEvent.ACTION_UP) {
            if (mSettledCentralPosition == getAdapter().getItemCount() - 1 &&
                    mInitialTouchY - event.getY() > OVERSCROLL_ADJUST_ROW_PX) {
                mStatusListener.onScrollStatusChanged(ScrollStatus.ADJUST_NEXT_ROW);
            } else if (mSettledCentralPosition == 0 &&
                    event.getY() - mInitialTouchY > OVERSCROLL_ADJUST_ROW_PX) {
                mStatusListener.onScrollStatusChanged(ScrollStatus.ADJUST_PREVIOUS_ROW);
            }

            mSettledCentralPosition = mScrollingCentralPosition;
        }

        return super.onTouchEvent(event);
    }

    public int getCentralPosition() {
        return mScrollingCentralPosition;
    }

    public void setScrollStatusListener(ScrollStatusListener listener) {
        mStatusListener = listener;

        addOnScrollListener(new WearableListView.OnScrollListener() {
            @Override
            public void onScroll(int scroll) {
            }
            @Override
            public void onAbsoluteScrollChange(int scroll) {
            }

            @Override
            public void onScrollStateChanged(int scrollState) {
                if (scrollState == RecyclerView.SCROLL_STATE_IDLE) {
                    mStatusListener.onScrollStatusChanged(ScrollStatus.SCROLL_IDLE);
                } else {
                    mStatusListener.onScrollStatusChanged(ScrollStatus.SCROLL_NOT_IDLE);
                }
            }

            @Override
            public void onCentralPositionChanged(int centralPosition) {
                mScrollingCentralPosition = centralPosition;
            }
        });
    }

    public interface ScrollStatusListener {
        void onScrollStatusChanged(ScrollStatus status);
    }
}
