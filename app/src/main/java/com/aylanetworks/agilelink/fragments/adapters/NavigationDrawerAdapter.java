package com.aylanetworks.agilelink.fragments.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import com.aylanetworks.agilelink.R;

import java.util.ArrayList;

/**
 * Created by user on 4/26/15.
 */
public class NavigationDrawerAdapter extends BaseExpandableListAdapter{

    public ArrayList<MenuItem> drawerGrp;
    public ArrayList<ArrayList<MenuItem>> moreChildren;
    public ArrayList<MenuItem> currentChild;
    public Activity mActivity;
    public Context mContext;
    public LayoutInflater mInflater;


    public NavigationDrawerAdapter(Context context, ArrayList<MenuItem> drawerGrp, ArrayList<ArrayList<MenuItem>> moreChildren, LayoutInflater inflater, Activity activity){
        this.drawerGrp = drawerGrp;
        this.moreChildren = moreChildren;
        this.mContext = context;
        this.mInflater = inflater;
        this.mActivity = activity;
    }
    @Override
    public int getGroupCount() {
        return drawerGrp.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        if(moreChildren.get(groupPosition) == null){
            return 0;
        }
        return moreChildren.get(groupPosition).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return null;
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return null;
    }

    @Override
    public long getGroupId(int groupPosition) {
        return 0;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {

        if(convertView == null){
           convertView = new TextView(mContext);

        }

        TextView groupText = (TextView) convertView;
        setTextViewStyle(groupText);
        groupText.setText(drawerGrp.get(groupPosition).getTitle());
        convertView.setTag(drawerGrp.get(groupPosition).getTitle());
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        currentChild = (ArrayList < MenuItem>)moreChildren.get(groupPosition);

        if(convertView == null){
            convertView = new TextView(mContext);
        }
        TextView childView = (TextView) convertView;
        setChildTextViewStyle(childView);
        if(currentChild == null){
            return null;
        }
        else{
            childView.setText(currentChild.get(childPosition).getTitle());
            childView.setTag(currentChild.get(childPosition));
            return convertView;
        }

    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public void setTextViewStyle(TextView view){
        view.setTextAppearance(mContext, R.style.DeviceListItemTextView);
        view.setTextColor(mContext.getResources().getColor(R.color.navigation_drawer_text));
        AbsListView.LayoutParams layoutParams = new AbsListView.LayoutParams( AbsListView.LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.MATCH_PARENT);
        view.setLayoutParams(layoutParams);


    }
    public void setChildTextViewStyle(TextView view){
        view.setTextAppearance(mContext, R.style.Submenu_text);
        view.setTextColor(mContext.getResources().getColor(R.color.navigation_drawer_text));
        AbsListView.LayoutParams params = new AbsListView.LayoutParams(AbsListView.LayoutParams.WRAP_CONTENT, AbsListView.LayoutParams.WRAP_CONTENT);
        view.setLayoutParams(params);


    }
}
