package com.aylanetworks.agilelink.fragments.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import com.aylanetworks.agilelink.R;

/**
 * Created by Brian King on 4/29/15.
 */
public class NestedMenuAdapter extends BaseExpandableListAdapter {
    private Context _context;
    private int _layoutId;
    private int _textViewId;
    private Menu _menu;

    public NestedMenuAdapter(Context context, int layoutId, int textViewId, Menu menu) {
        _context = context;
        _layoutId = layoutId;
        _textViewId = textViewId;
        _menu = menu;
    }

    @Override
    public int getGroupCount() {
        return _menu.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        MenuItem item = _menu.getItem(groupPosition);
        if ( item.hasSubMenu() ) {
            return item.getSubMenu().size();
        }
        return 0;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return _menu.getItem(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return _menu.getItem(groupPosition).getSubMenu().getItem(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return _menu.getItem(groupPosition).getItemId();
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return _menu.getItem(groupPosition).getSubMenu().getItem(childPosition).getItemId();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        if ( convertView == null ) {
            LayoutInflater inflater = (LayoutInflater)_context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(_layoutId, parent, false);
        }
        TextView textView = (TextView)convertView.findViewById(_textViewId);
        textView.setText(_menu.getItem(groupPosition).getTitle());

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        if ( convertView == null ) {
            LayoutInflater inflater = (LayoutInflater)_context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(_layoutId, parent, false);
            View indentView = convertView.findViewById(R.id.indent);
            indentView.setVisibility(View.INVISIBLE);
        }

        TextView textView = (TextView)convertView.findViewById(_textViewId);
        textView.setText(_menu.getItem(groupPosition).getSubMenu().getItem(childPosition).getTitle());

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}
