/**
 *   ownCloud Android client application
 *
 *   Copyright (C) 2012 Bartek Przybylski
 *   Copyright (C) 2012-2015 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.synox.android.ui.fragment;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.synox.android.R;
import com.synox.android.lib.common.utils.Log_OC;
import com.synox.android.ui.activity.OnEnforceableRefreshListener;
import com.synox.android.widgets.CustomRecyclerView;

public class ExtendedListFragment extends Fragment
        implements OnEnforceableRefreshListener{

    private static final String TAG = ExtendedListFragment.class.getSimpleName();

    private static final String KEY_EMPTY_LIST_MESSAGE = "EMPTY_LIST_MESSAGE";
    private static final String KEY_LIST_STATE = "LIST_STATE";
    protected static final int NUMBER_OF_GRID_COLUMNS = 2;
    protected static final int NUMBER_OF_GRID_COLUMNS_LANDSCAPE = 4;

    private SwipeRefreshLayout mRefreshListLayout;

    private SwipeRefreshLayout.OnRefreshListener mOnRefreshListener = null;
    private TextView mEmptyListMessage;

    protected SharedPreferences mAppPreferences;
    protected CustomRecyclerView mCurrentListView;
    protected RecyclerView.LayoutManager mLayoutManager;

    protected FloatingActionMenu fabMenu;
    protected FloatingActionButton newFileFab;
    protected FloatingActionButton newFolderFab;

    private int layout = 0;

    protected void setListAdapter(RecyclerView.Adapter listAdapter) {
        mCurrentListView.setAdapter(listAdapter);
        mCurrentListView.invalidate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            setMessageForEmptyList(savedInstanceState.getString(KEY_EMPTY_LIST_MESSAGE));
            mCurrentListView.getLayoutManager().onRestoreInstanceState(savedInstanceState.getParcelable(KEY_LIST_STATE));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log_OC.d(TAG, "onCreateView");
        setRetainInstance(true);

        mAppPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        View v = inflater.inflate(R.layout.list_fragment, container, false);
        mCurrentListView = (CustomRecyclerView)(v.findViewById(R.id.list_root));

        layout = mAppPreferences.getInt("viewLayout", R.layout.list_item);
        switch (layout) {
            case R.layout.list_item:
                mLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
                break;
            case R.layout.grid_item:
                if (getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    mLayoutManager = new GridLayoutManager(getActivity(), NUMBER_OF_GRID_COLUMNS, GridLayoutManager.VERTICAL, false);
                } else {
                    mLayoutManager = new GridLayoutManager(getActivity(), NUMBER_OF_GRID_COLUMNS_LANDSCAPE, GridLayoutManager.VERTICAL, false);
                }
                break;
        }

        // Fix for the bug when updating applications
        if (mLayoutManager == null)
        {
            if (mAppPreferences != null) {
                mAppPreferences.edit().clear().commit();
            }
            mLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        }

        mCurrentListView.setLayoutManager(mLayoutManager);

        // Floating action button
        fabMenu = (FloatingActionMenu) v.findViewById(R.id.fab_menu);
        newFileFab = (FloatingActionButton) v.findViewById(R.id.fab);
        newFolderFab = (FloatingActionButton) v.findViewById(R.id.fab2);

        mEmptyListMessage = (TextView)v.findViewById(R.id.empty_view);
        mCurrentListView.setEmptyView(mEmptyListMessage);
        mCurrentListView.setItemAnimator(new DefaultItemAnimator());
        mCurrentListView.setHasFixedSize(true);

        RecyclerView.ItemAnimator animator = mCurrentListView.getItemAnimator();

        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }

        mCurrentListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (Math.abs(dy) > 6 && fabMenu != null) {
                    if (dy > 0) {
                        fabMenu.hideMenu(true);
                    } else {
                        fabMenu.showMenu(true);
                    }
                }
            }
        });

        // Swipe to refresh list
        mRefreshListLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_containing_list);
        onCreateSwipeToRefresh(mRefreshListLayout);

        return v;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        Log_OC.v(TAG, "onConfigurationChanged() start");

        // Change column count for landscape mode in grid layout.
        if (mAppPreferences.getInt("viewLayout", R.layout.list_item) == R.layout.grid_item) {
            if (getActivity().getResources().getBoolean(R.bool.is_landscape))
            {
                mLayoutManager = new GridLayoutManager(getActivity(), NUMBER_OF_GRID_COLUMNS_LANDSCAPE, GridLayoutManager.VERTICAL, false);
            } else
            {
                mLayoutManager = new GridLayoutManager(getActivity(), NUMBER_OF_GRID_COLUMNS, GridLayoutManager.VERTICAL, false);
            }

            mCurrentListView.setLayoutManager(mLayoutManager);
        }

        // call onSaveInstanceState to save RecyclerView position on orientation change
        //onSaveInstanceState(new Bundle());

        Log_OC.v(TAG, "onConfigurationChanged() end");
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        Log_OC.d(TAG, "onSaveInstanceState() ");
        savedInstanceState.putString(KEY_EMPTY_LIST_MESSAGE, getEmptyViewText());
        savedInstanceState.putParcelable(KEY_LIST_STATE, mCurrentListView.getLayoutManager().onSaveInstanceState());
    }

    public CustomRecyclerView getListView()
    {
        return mCurrentListView;
    }

    @Override
    public void onRefresh() {
        mRefreshListLayout.setRefreshing(false);

        if (mOnRefreshListener != null) {
            mOnRefreshListener.onRefresh();
        }
    }
    public void setOnRefreshListener(OnEnforceableRefreshListener listener) {
        mOnRefreshListener = listener;
    }

    /**
     * Disables swipe gesture.
     *
     * Sets the 'enabled' state of the refresh layouts contained in the fragment.
     *
     * When 'false' is set, prevents user gestures but keeps the option to refresh programatically,
     *
     * @param   enabled     Desired state for capturing swipe gesture.
     */
    public void setSwipeEnabled(boolean enabled) {
        mRefreshListLayout.setEnabled(enabled);
    }

    /**
     * Set message for empty list view
     */
    public void setMessageForEmptyList(String message) {
        if (mEmptyListMessage != null) {
            mEmptyListMessage.setText(message);
            mCurrentListView.setEmptyView(mEmptyListMessage);
        }
    }

    /**
     * Get the text of EmptyListMessage TextView
     *
     * @return String
     */
    public String getEmptyViewText() {
        return (mEmptyListMessage != null) ? mEmptyListMessage.getText().toString() : "";
    }

    /**
     * On swipe to refresh
     * @param refreshLayout
     */
    private void onCreateSwipeToRefresh(SwipeRefreshLayout refreshLayout) {
        // Colors in animations
        refreshLayout.setColorSchemeResources(R.color.accent, R.color.primary,
                R.color.primary_dark);

        refreshLayout.setOnRefreshListener(this);
    }

    /**
     * Cancel refresh
     * @param ignoreETag
     */
    @Override
    public void onRefresh(boolean ignoreETag) {
        mRefreshListLayout.setRefreshing(false);

        if (mOnRefreshListener != null) {
            mOnRefreshListener.onRefresh();
        }
    }
}
