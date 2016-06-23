package com.aylanetworks.agilelinkwear;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.wearable.view.CardFragment;
import android.support.wearable.view.FragmentGridPagerAdapter;
import android.support.wearable.view.GridPagerAdapter;

import java.util.List;

public class DevicesGridAdapter extends FragmentGridPagerAdapter {

    private Context mContext;
    private List mRows;

    public DevicesGridAdapter(Context ctx, FragmentManager fm) {
        super(fm);
        mContext = ctx;
    }

    // A simple container for static data in each page
    private static class Page {
        // static resources
        int titleRes;
        int textRes;
        int iconRes;
    }

    // Create a static set of pages in a 2D array
    private final Page[][] PAGES = {};

    @Override
    public Fragment getFragment(int row, int col) {
        Page page = PAGES[row][col];
        String title =
        page.titleRes != 0 ? mContext.getString(page.titleRes) : null;
        String text =
        page.textRes != 0 ? mContext.getString(page.textRes) : null;
        CardFragment fragment = CardFragment.create(title, text, page.iconRes);

        // Advanced settings (card gravity, card expansion/scrolling)
        fragment.setCardGravity(page.cardGravity);
        fragment.setExpansionEnabled(page.expansionEnabled);
        fragment.setExpansionDirection(page.expansionDirection);
        fragment.setExpansionFactor(page.expansionFactor);
        return fragment;
    }

    @Override
    public Drawable getBackgroundForPage(int row, int column) {
        return GridPagerAdapter.BACKGROUND_NONE;
    }

    @Override
    public int getRowCount() {
        return PAGES.length;
    }

    @Override
    public int getColumnCount(int rowNum) {
        return PAGES[rowNum].length;
    }
}
